/*
*
* Developed and adpated by Songjie Wang
* Department of EECS
* University of Missouri
*
*/

package org.example.basicApp.client;

import java.net.UnknownHostException;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.amazonaws.services.kinesis.model.ResourceNotFoundException;
import org.example.basicApp.model.VrMeasurement;
import org.example.basicApp.utils.DynamoDBUtils;
import org.example.basicApp.utils.SampleUtils;
import org.example.basicApp.utils.StreamUtils;

// Amazon Kinesis client application to digest stream data
public class MeasurementProcessor {
	
    private static final Log LOG = LogFactory.getLog(MeasurementProcessor.class);
   
    public static final String SAMPLE_APPLICATION_STREAM_NAME = "wangso-stream";
    private static final String SAMPLE_APPLICATION_NAME = "clientApp";
    
    // Start the Kinesis Client application.
    public static void main(String[] args) throws UnknownHostException {
        if (args.length != 4) {
            System.err.println("Usage: " + MeasurementProcessor.class.getSimpleName()
                    + " <application name> <stream name> <DynamoDB table name> <region>");
            System.exit(1);
        }

        String applicationName = args[0];
        String streamName = args[1];
        String dynamoTableName = args[2];
        Region region = SampleUtils.parseRegion(args[3]);

        //setup AWS credentials and user
        AWSCredentialsProvider credentialsProvider = new DefaultAWSCredentialsProviderChain();
        ClientConfiguration clientConfig = SampleUtils.configureUserAgentForSample(new ClientConfiguration());
        AmazonKinesis kinesis = new AmazonKinesisClient(credentialsProvider, clientConfig);
        kinesis.setRegion(region);
        AmazonDynamoDB dynamoDB = new AmazonDynamoDBClient(credentialsProvider, clientConfig);
        dynamoDB.setRegion(region);
         
        // Creates a stream to read from, if it does not exist
        StreamUtils streamUtils = new StreamUtils(kinesis);
        streamUtils.createStreamIfNotExists(streamName, 2);
        LOG.info(String.format("%s stream is ready for use", streamName));
        
        // Create a dynamoDB table if it does not exist
        DynamoDBUtils dynamoDBUtils = new DynamoDBUtils(dynamoDB);
        dynamoDBUtils.createDynamoTableIfNotExists(dynamoTableName);
        LOG.info(String.format("DynamoDB table %s is ready for use", dynamoTableName));

        // create a client worker ID
        String workerId = String.valueOf(UUID.randomUUID());
        LOG.info(String.format("Using working id: %s", workerId));
        
        // setup client configuration
        KinesisClientLibConfiguration kclConfig =
                new KinesisClientLibConfiguration(applicationName, streamName, credentialsProvider, workerId);
        kclConfig.withCommonClientConfig(clientConfig);
        kclConfig.withRegionName(region.getName());
        kclConfig.withInitialPositionInStream(InitialPositionInStream.LATEST);
        
        // create a DB writer
        DynamoDBMeasurementWriter dbWriter =
                new DynamoDBMeasurementWriter(dynamoDB,dynamoTableName);         
        
        // create a new processor factory to generate processors
        IRecordProcessorFactory recordProcessorFactory =
                new MeasurementRecordProcessorFactory(dbWriter);

        // create a worker based on customized configuration
        Worker worker = new Worker(recordProcessorFactory, kclConfig);

        // let the worker start to work
        int exitCode = 0;
        try {
            worker.run();
        } catch (Throwable t) {
            LOG.error("Caught throwable while processing data.", t);
            exitCode = 1;
        }
        System.exit(exitCode);
    }
}
