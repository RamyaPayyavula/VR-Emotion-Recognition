/*
*
* Developed by Songjie Wang
* Department of EECS
* University of Missouri
*
*/

package org.example.basicApp.ddb;

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
import org.example.basicApp.client.MeasurementProcessor;
import org.example.basicApp.model.DdbRecordToWrite;
import org.example.basicApp.model.SingleMeasurementValue;
import org.example.basicApp.model.VrMeasurement;
import org.example.basicApp.utils.DynamoDBUtils;
import org.example.basicApp.utils.SampleUtils;
import org.example.basicApp.utils.StreamUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
//import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClient;

/**
 * This sample demonstrates how to perform a few simple operations with the
 * Amazon DynamoDB service.
 */
public class DynamoDBWriter {
	
    private static final Log LOG = LogFactory.getLog(DynamoDBWriter.class);
	private static final float deviation = 0.1f;
    static AmazonDynamoDB dynamoDB;
    private static final int numUsers=1;
    private static DdbRecordToWrite ddbRecordToWrite;
    
    public static void main(String[] args) throws Exception {
    	
        if(args.length != 2) {
        System.err.println("Usage: " + DynamoDBWriter.class.getSimpleName()
                + "<DynamoDB table name> <region>");
        System.exit(1);
        }
        
	    String dynamoTableName = args[0];
	    Region region = SampleUtils.parseRegion(args[1]);
	    
        AWSCredentialsProvider credentialsProvider = new DefaultAWSCredentialsProviderChain();
        ClientConfiguration clientConfig = SampleUtils.configureUserAgentForSample(new ClientConfiguration());
        AmazonKinesis kinesis = new AmazonKinesisClient(credentialsProvider, clientConfig);
        kinesis.setRegion(region);
        AmazonDynamoDB dynamoDB = new AmazonDynamoDBClient(credentialsProvider, clientConfig);
        dynamoDB.setRegion(region);
         
        DynamoDBUtils dynamoDBUtils = new DynamoDBUtils(dynamoDB);
        dynamoDBUtils.createDynamoTableIfNotExists(dynamoTableName);
        LOG.info(String.format("%s DynamoDB table is ready for use", dynamoTableName));
	
        // Describe our new table
        DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(dynamoTableName);
        TableDescription tableDescription = dynamoDB.describeTable(describeTableRequest).getTable();
        System.out.println("Table Description: " + tableDescription);
        
        try {
        	
            while(true) {
	            
	            // Add an item
	            VrMeasurement measurementRecord =  new VrMeasurement();
	            System.out.printf("record ready to put into DynamoDB is: %s \n" , measurementRecord.toString());

	            for (int i=1; i<numUsers+1; i++) {
	            	ddbRecordToWrite = generateDBRecord(measurementRecord, "user"+i);
	                System.out.printf("record ready to write for user %s is: %s \n" ,i, ddbRecordToWrite.toString());

		            Map<String, AttributeValue> item = newItem(ddbRecordToWrite);
	//	            for (Map.Entry entry : item.entrySet())
	//	            {
	//	                System.out.println("key: " + entry.getKey() + "; value: " + entry.getValue());
	//	            }
		            
		            PutItemRequest putItemRequest = new PutItemRequest(dynamoTableName, item);
		            PutItemResult putItemResult = dynamoDB.putItem(putItemRequest);
		            System.out.println("Result: " + putItemResult);	 
		            
		            
	            }
	            Thread.sleep(1000);
            }
        } catch (AmazonServiceException ase) {
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
    
    private static DdbRecordToWrite generateDBRecord(VrMeasurement measurementRecord, String user) {
    	
    	DdbRecordToWrite ddbRecordToWrite = new DdbRecordToWrite();
        ddbRecordToWrite.setResource(measurementRecord.getResource());
        //ddbRecordToWrite.setTimeStamp(measurementRecord.getTimeStamp());
        ddbRecordToWrite.setHost(user);
        
        Date date = new Date();
        date.setTime(System.currentTimeMillis());
        ddbRecordToWrite.setTimeStamp(toISO8601UTC(date));
        
    	List<SingleMeasurementValue> measurementValues = new ArrayList<SingleMeasurementValue>();
       	SingleMeasurementValue value1 = new SingleMeasurementValue("{\"measurement\":\"engagement\",\"value\":", getRandomFloat(0.9f),"}");
    	SingleMeasurementValue value2 = new SingleMeasurementValue("{\"measurement\":\"focus\",\"value\":", getRandomFloat(0.8f),"}");
    	SingleMeasurementValue value3 = new SingleMeasurementValue("{\"measurement\":\"excitement\",\"value\":", getRandomFloat(0.7f),"}");
    	SingleMeasurementValue value4 = new SingleMeasurementValue("{\"measurement\":\"frustration\",\"value\":", getRandomFloat(0.2f),"}");
    	SingleMeasurementValue value5 = new SingleMeasurementValue("{\"measurement\":\"stress\",\"value\":", getRandomFloat(0.1f),"}");
    	SingleMeasurementValue value6 = new SingleMeasurementValue("{\"measurement\":\"relaxation\",\"value\":", getRandomFloat(0.5f),"}");
    	measurementValues.add(value1);
		measurementValues.add(value2);
		measurementValues.add(value3);
		measurementValues.add(value4);
		measurementValues.add(value5);
		measurementValues.add(value6);
		
        ddbRecordToWrite.setValues(measurementValues);		            	            
        

    	return ddbRecordToWrite;
    }
    
    
    
    private static Map<String, AttributeValue> newItem(DdbRecordToWrite record) {
    	
    	Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put("resource", new AttributeValue(record.getResource()));
        item.put("timestamp", new AttributeValue(record.getTimeStamp()));
        item.put("host", new AttributeValue(record.getHost()));
        item.put("values", new AttributeValue(record.getValues().toString()));


        return item;
    }
    
    public static String toISO8601UTC(Date date) {
    	  TimeZone tz = TimeZone.getTimeZone("UTC");
    	  DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    	  df.setTimeZone(tz);
    	  return df.format(date);
    	}
        
    public static Float getRandomFloat(Float mean) {
    	
    	Random rand = new Random();	
    	// set the price using the deviation and mean price
        
    	Float max = mean + deviation;
    	Float min = mean - deviation;

        // randomly pick a quantity of shares
        Float value = rand.nextFloat() * (max - min) + min; 

        return value;
    }
	
}
