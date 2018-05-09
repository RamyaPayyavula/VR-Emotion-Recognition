package org.example.basicApp;

import java.io.File;
//import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
//import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

//import javax.xml.bind.JAXBElement.GlobalScope;

import org.example.basicApp.ddb.DynamoDBWriter;
import org.example.basicApp.model.DdbRecordToWrite;
import org.example.basicApp.model.SingleMeasurementValue;
import org.example.basicApp.model.VrMeasurement;
import org.example.basicApp.utils.DynamoDBUtils;
import org.example.basicApp.utils.SampleUtils;

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
//import com.amazonaws.services.kinesis.AmazonKinesis;
//import com.amazonaws.services.kinesis.AmazonKinesisClient;


import oscP5.*;

//import java.util.HashMap;
//import java.util.Map; 

public class MuseOscServer {

    static MuseOscServer museOscServer;
    OscP5 museServer = null;    
    static int recvPort = 5000;
    
	static AmazonDynamoDB dynamoDB;
    private static final int numUsers=1;
    private static DdbRecordToWrite ddbRecordToWrite;
    private static String dynamoTableName;
    private static SingleMeasurementValue value1, value2, value3, value4, value5, value6;
    
    public static void main(String[] args) throws FileNotFoundException {
    	
//    	File filename = new File("trial.csv");
//    	PrintStream o = new PrintStream(filename);
//    	System.setOut(o);
    	
    	if(args.length != 2) {
            System.err.println("Usage: " + DynamoDBWriter.class.getSimpleName()
                    + "<DynamoDB table name> <region>");
            System.exit(1);
            }
            
    	    dynamoTableName = args[0];
    	    Region region = SampleUtils.parseRegion(args[1]);
    	    
            AWSCredentialsProvider credentialsProvider = new DefaultAWSCredentialsProviderChain();
            ClientConfiguration clientConfig = SampleUtils.configureUserAgentForSample(new ClientConfiguration());
           // AmazonKinesis kinesis = new AmazonKinesis(credentialsProvider);
           // kinesis.setRegion(region);
            dynamoDB = new AmazonDynamoDBClient(credentialsProvider, clientConfig);
            dynamoDB.setRegion(region);
             
            DynamoDBUtils dynamoDBUtils = new DynamoDBUtils(dynamoDB);
            dynamoDBUtils.createDynamoTableIfNotExists(dynamoTableName);
            //LOG.info(String.format("%s DynamoDB table is ready for use", dynamoTableName));
    	
            // Describe our new table
            DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(dynamoTableName);
            TableDescription tableDescription = dynamoDB.describeTable(describeTableRequest).getTable();
            System.out.println("Table Description: " + tableDescription);

    	museOscServer = new MuseOscServer();
    	museOscServer.museServer = new OscP5(museOscServer,recvPort);
    }
    void oscEvent(OscMessage msg) throws IOException{

    	System.out.println("Start receiving EEG Data!");       
        Object[] arguments=msg.arguments();

        float avg1= 0.0f;
        float avg2=0.0f;
        float avg3=0.0f;
        float avg4=0.0f;
        float avg5=0.0f;
        float avg6=0.0f;
        
        
		Double A = (Double)arguments[0];
		Double B = (Double)arguments[1];
		Double C = (Double)arguments[2];
		Double D = (Double)arguments[3];
		
		float AA = A.floatValue();
		float BB = B.floatValue();
		float CC = C.floatValue();
		float DD = D.floatValue();		
		
       System.out.printf("AA = %f \n",AA);
       System.out.printf("BB = %f \n",BB);
       System.out.printf("CC = %f \n",CC);
       System.out.printf("DD = %f \n",DD);
    	
 	
    	if(msg.checkAddrPattern("/elements/alpha_absolute")==true) {
        	System.out.println("I am here at 1!");       

    		avg1 = (AA + BB + CC+ DD)/4.0f;
    		if(Float.compare(avg1,1.0f)==1 || Float.compare(avg1,0.0f)== -1) {avg1=0.0f;}
    		value1 = new SingleMeasurementValue("{\"measurement\":\"alpha\",\"value\":",avg1,"}");
       		}
    	if(msg.checkAddrPattern("/elements/beta_absolute")==true) {
        	System.out.println("I am here at 2!");       

    		avg2 = (AA + BB + CC+ DD)/4.0f;
    		if(Float.compare(avg2,1.0f)==1 || Float.compare(avg2,0.0f)== -1) {avg2=0.0f;}

    		 value2 = new SingleMeasurementValue("{\"measurement\":\"beta\",\"value\":",avg2,"}");
    		}
    	if(msg.checkAddrPattern("/elements/gamma_absolute")==true) {
        	System.out.println("I am here at 3!");       

    		avg3 = (AA + BB + CC+ DD)/4.0f;
    		if(Float.compare(avg3,1.0f)==1 || Float.compare(avg3,0.0f)== -1) {avg3=0.0f;}

    		 value3 = new SingleMeasurementValue("{\"measurement\":\"gamma\",\"value\":",avg3,"}");
    		}
    	if(msg.checkAddrPattern("/elements/delta_absolute")==true) {
        	System.out.println("I am here at 4!");       

    		avg4 = (AA + BB + CC+ DD)/4.0f;
    		if(Float.compare(avg4,1.0f)==1 || Float.compare(avg4,0.0f)== -1) {avg4=0.0f;}

    		 value4 = new SingleMeasurementValue("{\"measurement\":\"delta\",\"value\":",avg4,"}");
    		}
    	if(msg.checkAddrPattern("/elements/theta_absolute")==true) {
        	System.out.println("I am here at 5!");       

    		avg5 = (AA + BB + CC+ DD)/4.0f;
    		if(Float.compare(avg5,1.0f)==1 || Float.compare(avg5,0.0f)== -1) {avg5=0.0f;}

    		 value5 = new SingleMeasurementValue("{\"measurement\":\"theta\",\"value\":",avg5,"}");
    		
        	System.out.println("I am here at 6!");       

    		avg6 = 0.8f;
//    		if(Float.compare(avg6,1.0f)==1 || Float.compare(avg6,0.0f)== -1) {avg6=0.0f;}

    		value6 = new SingleMeasurementValue("{\"measurement\":\"eeg\",\"value\":",avg6,"}");
    		
    		ArrayList<SingleMeasurementValue> measurementValues = new ArrayList<SingleMeasurementValue>();
    		measurementValues.add(value1);
    		measurementValues.add(value2);
    		measurementValues.add(value3);
    		measurementValues.add(value4);
    		measurementValues.add(value5);
    		measurementValues.add(value6);
    	
	        for (int i=1; i<numUsers+1; i++) {
	            	ddbRecordToWrite = generateDBRecord(measurementValues, "user"+i);
	
	                System.out.printf("record ready to write for user %s is: %s \n" ,i, ddbRecordToWrite.toString());
	
	                Map<String, AttributeValue> item = newItem(ddbRecordToWrite);			            
	                System.out.println(""+item.get("resource")+",");  
	                        
	                System.out.print(""+item.get("timestamp")+",");                    
	                System.out.print(""+item.get("host")+",");                    
	                System.out.print(""+item.get("values"));                    
	                System.out.println("\n");
				            	
				    try {        	
		                PutItemRequest putItemRequest = new PutItemRequest(dynamoTableName, item);
		
			            PutItemResult putItemResult = dynamoDB.putItem(putItemRequest);
		
			            System.out.println("Result: " + putItemResult);	
				    } catch (Exception ex) {
				    	ex.printStackTrace();
				    }
		            		            
		    }
	        
	        try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
	        
    	}
	             
	 }

    private static DdbRecordToWrite generateDBRecord(ArrayList<SingleMeasurementValue> measurementValues,  String user) {
    	
       // VrMeasurement measurementRecord =  new VrMeasurement();	            

    	DdbRecordToWrite ddbRecordToWrite = new DdbRecordToWrite();
        ddbRecordToWrite.setResource("Muse Sensor");
        //ddbRecordToWrite.setTimeStamp(measurementRecord.getTimeStamp());
        ddbRecordToWrite.setHost(user);
        
        Date date = new Date();
        date.setTime(System.currentTimeMillis());
        ddbRecordToWrite.setTimeStamp(toISO8601UTC(date));
        
  
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
}