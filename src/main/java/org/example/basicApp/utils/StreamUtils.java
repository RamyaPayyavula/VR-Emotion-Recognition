/*
*
* Developed and adpated by Songjie Wang
* Department of EECS
* University of Missouri
*
*/

package org.example.basicApp.utils;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.model.DescribeStreamResult;
import com.amazonaws.services.kinesis.model.InvalidArgumentException;
import com.amazonaws.services.kinesis.model.LimitExceededException;
import com.amazonaws.services.kinesis.model.ResourceNotFoundException;
import com.amazonaws.services.kinesis.model.Shard;
import com.amazonaws.services.kinesis.model.StreamDescription;

// A collection of functions to manipulate Amazon Kinesis streams.
public class StreamUtils {
    private static final Log LOG = LogFactory.getLog(StreamUtils.class);

    private AmazonKinesis kinesis;
    private static final long CREATION_WAIT_TIME_IN_SECONDS = TimeUnit.SECONDS.toMillis(30);
    private static final long DELAY_BETWEEN_STATUS_CHECKS_IN_SECONDS = TimeUnit.SECONDS.toMillis(30);

    public StreamUtils(AmazonKinesis kinesis) {
        if (kinesis == null) {
            throw new NullPointerException("Amazon Kinesis client must not be null");
        }
        this.kinesis = kinesis;
    }

    // Create a stream if it doesn't already exist. 
    public void createStreamIfNotExists(String streamName, int shards) throws AmazonClientException {
        try {
            if (isActive(kinesis.describeStream(streamName))) {
                return;
            }
        } catch (ResourceNotFoundException ex) {
            LOG.info(String.format("Creating stream %s...", streamName));
            // No stream, create
            kinesis.createStream(streamName, shards);
            // Initially wait a number of seconds before checking for completion
            try {
                Thread.sleep(CREATION_WAIT_TIME_IN_SECONDS);
            } catch (InterruptedException e) {
                LOG.warn(String.format("Interrupted while waiting for %s stream to become active. Aborting.", streamName));
                Thread.currentThread().interrupt();
                return;
            }
        }

        // Wait for stream to become active
        int maxRetries = 3;
        int i = 0;
        while (i < maxRetries) {
            i++;
            try {
                if (isActive(kinesis.describeStream(streamName))) {
                    return;
                }
            } catch (ResourceNotFoundException ignore) {
                // The stream may be reported as not found if it was just created.
            }
            try {
                Thread.sleep(DELAY_BETWEEN_STATUS_CHECKS_IN_SECONDS);
            } catch (InterruptedException e) {
                LOG.warn(String.format("Interrupted while waiting for %s stream to become active. Aborting.", streamName));
                Thread.currentThread().interrupt();
                return;
            }
        }
        throw new RuntimeException("Stream " + streamName + " did not become active within 2 minutes.");
    }

    // check to see if stream is active
    private boolean isActive(DescribeStreamResult r) {
        return "ACTIVE".equals(r.getStreamDescription().getStreamStatus());
    }

    // Delete an Amazon Kinesis stream.
    public void deleteStream(String streamName) {
        LOG.info(String.format("Deleting Kinesis stream %s", streamName));
        try {
            kinesis.deleteStream(streamName);
        } catch (ResourceNotFoundException ex) {
            // The stream could not be found.
        } catch (AmazonClientException ex) {
            LOG.error(String.format("Error deleting stream %s", streamName), ex);
        }
    }

    // Split a shard by dividing the hash key space in half.
    public void splitShardEvenly(String streamName, String shardId)
        throws LimitExceededException, ResourceNotFoundException, AmazonClientException, InvalidArgumentException,
        IllegalArgumentException {
        if (streamName == null || streamName.isEmpty()) {
            throw new IllegalArgumentException("stream name is required");
        }
        if (shardId == null || shardId.isEmpty()) {
            throw new IllegalArgumentException("shard id is required");
        }

        DescribeStreamResult result = kinesis.describeStream(streamName);
        StreamDescription description = result.getStreamDescription();

        // Find the shard we want to split
        Shard shardToSplit = null;
        for (Shard shard : description.getShards()) {
            if (shardId.equals(shard.getShardId())) {
                shardToSplit = shard;
                break;
            }
        }

        if (shardToSplit == null) {
            throw new ResourceNotFoundException("Could not find shard with id '" + shardId + "' in stream '"
                    + streamName + "'");
        }

        // Check if the shard is still open. Open shards do not have an ending sequence number.
        if (shardToSplit.getSequenceNumberRange().getEndingSequenceNumber() != null) {
            throw new InvalidArgumentException("Shard is CLOSED and is not eligible for splitting");
        }

        // Calculate the median hash key to use as the new starting hash key for the shard.
        BigInteger startingHashKey = new BigInteger(shardToSplit.getHashKeyRange().getStartingHashKey());
        BigInteger endingHashKey = new BigInteger(shardToSplit.getHashKeyRange().getEndingHashKey());
        BigInteger[] medianHashKey = startingHashKey.add(endingHashKey).divideAndRemainder(new BigInteger("2"));
        BigInteger newStartingHashKey = medianHashKey[0];
        if (!BigInteger.ZERO.equals(medianHashKey[1])) {
            // In order to more evenly distributed the new hash key ranges across the new shards we will "round up" to
            // the next integer when our current hash key range is not evenly divisible by 2.
            newStartingHashKey = newStartingHashKey.add(BigInteger.ONE);
        }

        // Submit the split shard request
        kinesis.splitShard(streamName, shardId, newStartingHashKey.toString());
    }
}
