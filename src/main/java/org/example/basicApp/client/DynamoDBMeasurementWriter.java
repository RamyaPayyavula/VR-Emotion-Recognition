/*
*
* Developed and adpated by Songjie Wang
* Department of EECS
* University of Missouri
*
*/

package org.example.basicApp.client;

import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper.FailedBatch;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import org.example.basicApp.model.VrMeasurement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.example.basicApp.model.DdbRecordToWrite;
import org.example.basicApp.model.SingleMeasurementValue;
import org.example.basicApp.model.VrMeasurement;
import org.example.basicApp.utils.DynamoDBUtils;
import org.example.basicApp.utils.SampleUtils;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClient;


// Persists counts to DynamoDB.
public class DynamoDBMeasurementWriter {
	
    private static final Log LOG = LogFactory.getLog(DynamoDBMeasurementWriter.class);

    // Generate UTC timestamps
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    // generate a dynamodb mapper
//    private DynamoDBMapper mapper;
    private AmazonDynamoDB dynamoDB;
    private String dynamoTable;

    //set up a maximum data entries in memory
    private static final int MAX_COUNTS_IN_MEMORY = 60000;

    // The queue holds all data records to be sent to DynamoDB.
    private BlockingQueue<Map<String, AttributeValue>> queue = new LinkedBlockingQueue<>(MAX_COUNTS_IN_MEMORY);

    // The thread to use for sending counts to DynamoDB.
    private Thread dynamoDBSender;


    // Create a new persister to send data to Amazon DynamoDB.
    public DynamoDBMeasurementWriter (AmazonDynamoDB dynamoDB, String dynamoTableName) {
        if (dynamoTableName == null) {
            throw new NullPointerException("dynamoTable must not be null");
        }
        this.dynamoDB = dynamoDB;
        this.dynamoTable = dynamoTableName;
    }

    public void initialize() {

        // This thread is responsible for draining the queue of data records and sending them in batches to DynamoDB
        dynamoDBSender = new Thread() {

            @Override
            public void run() {
                // Create a reusable buffer to drain our queue into.
                List<Map<String, AttributeValue>> buffer = new ArrayList<>(MAX_COUNTS_IN_MEMORY);

                // Continuously attempt to drain the queue and send to DynamoDB until this thread is interrupted
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        // Drain anything that's in the queue to the buffer and write the items to DynamoDB
                        sendQueueToDynamoDB(buffer);
                        // We wait for an empty queue before checkpointing. Notify that thread when we're empty in
                        // case it is waiting.
                        synchronized(queue) {
                            if (queue.isEmpty()) {
                            	queue.notify();
                            }
                        }
                    } catch (InterruptedException e) {
                        LOG.error("Thread that handles persisting counts to DynamoDB was interrupted!",
                                e);
                        return;
                    } finally {
                        // Clear the temporary buffer 
                        buffer.clear();
                    }
                }
            }
        };
        dynamoDBSender.setDaemon(true);
        dynamoDBSender.start();
    }

    public void pushToQueue (VrMeasurement measurementRecord) {
        if (measurementRecord == null) {
            return;
        }
        
        DdbRecordToWrite ddbRecordToWrite = new DdbRecordToWrite();
        
        // retrieve three attribute values of the data record
        ddbRecordToWrite.setResource(measurementRecord.getResource());
        ddbRecordToWrite.setTimeStamp(measurementRecord.getTimeStamp());
        ddbRecordToWrite.setHost(measurementRecord.getHost());
        
    	List<SingleMeasurementValue> measurementValues = new ArrayList<SingleMeasurementValue>();
       	SingleMeasurementValue value1 = new SingleMeasurementValue("{\"measurement\":\"engagement\",\"value\":", measurementRecord.getEngagement(),"}");
    	SingleMeasurementValue value2 = new SingleMeasurementValue("{\"measurement\":\"focus\",\"value\":", measurementRecord.getFocus(),"}");
    	SingleMeasurementValue value3 = new SingleMeasurementValue("{\"measurement\":\"excitement\",\"value\":", measurementRecord.getExcitement(),"}");
    	SingleMeasurementValue value4 = new SingleMeasurementValue("{\"measurement\":\"frustration\",\"value\":", measurementRecord.getFrustration(),"}");
    	SingleMeasurementValue value5 = new SingleMeasurementValue("{\"measurement\":\"stress\",\"value\":", measurementRecord.getStress(),"}");
    	SingleMeasurementValue value6 = new SingleMeasurementValue("{\"measurement\":\"relaxation\",\"value\":", measurementRecord.getRelaxation(),"}");
    	measurementValues.add(value1);
		measurementValues.add(value2);
		measurementValues.add(value3);
		measurementValues.add(value4);
		measurementValues.add(value5);
		measurementValues.add(value6);		
        ddbRecordToWrite.setValues(measurementValues);
        
        System.out.printf("record ready to persis into DynamoDB is: \n");
        Map<String, AttributeValue> item = newItem(ddbRecordToWrite);
        for (Map.Entry entry : item.entrySet())
        {      	
            System.out.println("key: " + entry.getKey() + "; value: " + entry.getValue());
        }

        queue.add(item);
    }
   
    // Block until the entire queue of counts has been drained.
    public void checkpoint() throws InterruptedException {
        // make sure all data are flushed to DynamoDB before we return successfully.
        if (dynamoDBSender.isAlive()) {
            // If the DynamoDB thread is running wait until data queue is empty
            synchronized(queue) {
                while (!queue.isEmpty()) {
                	queue.wait();
                }                
            }
        } else {
            throw new IllegalStateException("DynamoDB persister thread is not running!");
        }
    }

    // Drain the queue into the provided buffer and write data records to DynamoDB. 
    protected void sendQueueToDynamoDB(List<Map<String, AttributeValue>> buffer) throws InterruptedException {
        // Block while waiting for data
        buffer.add(queue.take());
        queue.drainTo(buffer);
        
        try {
        	
	        for (Map<String, AttributeValue> singleRecord : buffer) {
	            PutItemRequest putItemRequest = new PutItemRequest(dynamoTable, singleRecord);
	            PutItemResult putItemResult = dynamoDB.putItem(putItemRequest);
	            System.out.println("Result: one data record has been persisted into dynamoDB... \n"+ putItemResult);	 
	        }
	        
        }catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to AWS, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with AWS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }        
    }
     
    // generate map data structure from data records
    private static Map<String, AttributeValue> newItem(DdbRecordToWrite record) {

    	Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put("resource", new AttributeValue(record.getResource()));
        item.put("timestamp", new AttributeValue(record.getTimeStamp()));
        item.put("host", new AttributeValue(record.getHost()));
        item.put("values", new AttributeValue(record.getValues().toString()));
        return item;
    }
}
