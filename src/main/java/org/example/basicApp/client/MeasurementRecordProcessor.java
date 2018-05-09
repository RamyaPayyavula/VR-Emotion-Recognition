/*
*
* Developed and adpated by Songjie Wang
* Department of EECS
* University of Missouri
*
*/

package org.example.basicApp.client;

import java.io.IOException;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ThrottlingException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import org.example.basicApp.model.VrMeasurement;
//import org.example.basicApp.model.RawMeasurement;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;


// record processor to process records from stream
public class MeasurementRecordProcessor implements IRecordProcessor {

	private static final Log LOG = LogFactory.getLog(MeasurementRecordProcessor.class);
    
    // JSON object mapper for deserializing records
    private final ObjectMapper objectMapper;

    // The shard ID this processor is processing
    private String kinesisShardId;
    
    // This is responsible for writing record to dynamoDB every interval
    private DynamoDBMeasurementWriter dbWriter;

    // Backoff and retry settings
    private static final long BACKOFF_TIME_IN_MILLIS = 3000L;
    private static final int NUM_RETRIES = 10;

    // Checkpoint about once a minute
    private static final long CHECKPOINT_INTERVAL_MILLIS = 60000L;
    private long nextCheckpointTimeInMillis;
    
    
    // Create a new processor with the dbWriter to write data to dynamoDB    
    public MeasurementRecordProcessor(DynamoDBMeasurementWriter dbWriter) {

        if (dbWriter == null) {
            throw new NullPointerException("dbWriter must not be null");
        }
        
        this.dbWriter = dbWriter;
        // Create an object mapper to deserialize records that ignores unknown properties
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void initialize(String shardId) {
        LOG.info("Initializing record processor for shard: " + shardId);
        this.kinesisShardId = shardId;
        dbWriter.initialize();
    }

    @Override
    public void processRecords(List<Record> records, IRecordProcessorCheckpointer checkpointer) {
    	
        LOG.info("Processing " + records.size() + " records from " + kinesisShardId);
        
        for (Record record : records) {
            boolean processedSuccessfully = false;
            for (int i = 0; i < NUM_RETRIES; i++) {
                try {
                    processSingleRecord(record);
                    processedSuccessfully = true;
                    break;
                } catch (Throwable t) {
                    LOG.warn("Caught throwable while processing record " + record, t);
                }
                // backoff if encounter an exception.
                try {
                    Thread.sleep(BACKOFF_TIME_IN_MILLIS);
                } catch (InterruptedException e) {
                    LOG.debug("Interrupted sleep", e);
                }
            }

            if (!processedSuccessfully) {
                LOG.error("Couldn't process record " + record + ". Skipping the record.");
            }
        }

        // Checkpoint once every checkpoint interval.
        if (System.currentTimeMillis() > nextCheckpointTimeInMillis) {
            checkpoint(checkpointer);
            nextCheckpointTimeInMillis = System.currentTimeMillis() + CHECKPOINT_INTERVAL_MILLIS;
        }
    }

    // process a single record
    public void processSingleRecord(Record r) {
     
        // Deserialize each record as an encoded JSON String of the type provided
        VrMeasurement Data= null;
    	VrMeasurement data = new VrMeasurement();
        	
        try {
        	Data = objectMapper.readValue(r.getData().array(), VrMeasurement.class); 
        } catch (IOException e) {
           LOG.warn("Skipping record. Unable to parse record into Measurements. Partition Key: "
                + r.getPartitionKey() + ". Sequence Number: " + r.getSequenceNumber(),e);           
        }        
        // process Data and generate lightweighted data, and then persist the data record into queue
        if (Data != null) {     
        	LOG.info(String.format("one Data record has been retrieved from stream ..."));
        	data.setResource(Data.getResource());
        	data.setTimeStamp(Data.getTimeStamp());
        	
        	LOG.info(String.format("Data timestamp is : %s", Data.getTimeStamp()));
        	
        	data.setHost(Data.getHost());  
        	LOG.info(String.format("one record has been processed, processed data include: %s", data.toString()));
            dbWriter.pushToQueue(data);
        }        
     }

    @Override
    public void shutdown(IRecordProcessorCheckpointer checkpointer, ShutdownReason reason) {
        LOG.info("Shutting down record processor for shard: " + kinesisShardId);
        if (reason == ShutdownReason.TERMINATE) {
            try {
                checkpointer.checkpoint();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    // Checkpoint with retries.
    private void checkpoint(IRecordProcessorCheckpointer checkpointer) {
        LOG.info("Checkpointing shard " + kinesisShardId);
        for (int i = 0; i < NUM_RETRIES; i++) {
            try {
            	dbWriter.checkpoint();
                checkpointer.checkpoint();
                break;
            } catch (ShutdownException se) {
                // Ignore checkpoint if the processor instance has been shutdown (fail over).
                LOG.info("Caught shutdown exception, skipping checkpoint.", se);
                break;
            } catch (ThrottlingException e) {
                // Backoff and re-attempt checkpoint upon transient failures
                if (i >= (NUM_RETRIES - 1)) {
                    LOG.error("Checkpoint failed after " + (i + 1) + "attempts.", e);
                    break;
                } else {
                    LOG.info("Transient issue when checkpointing - attempt " + (i + 1) + " of "
                            + NUM_RETRIES, e);
                }
            } catch (InvalidStateException e) {
                // This indicates an issue with the DynamoDB table (check for table, provisioned IOPS).
                LOG.error("Cannot save checkpoint to the DynamoDB table used by the Amazon Kinesis Client Library.", e);
                break;
            }catch (InterruptedException e) {
                LOG.error("Error encountered while checkpointing dbWriter.", e);
            }
            try {
                Thread.sleep(BACKOFF_TIME_IN_MILLIS);
            } catch (InterruptedException e) {
                LOG.debug("Interrupted sleep", e);
            }
        }
    }    
}
