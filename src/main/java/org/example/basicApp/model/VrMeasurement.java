/*
*
* Developed by Songjie Wang
* Department of EECS
* University of Missouri
*
*/

package org.example.basicApp.model;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;

//import org.example.basicApp.ddb.MeasurementRecordMarshaller;
//import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
//import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
//import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMarshalling;
//import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
//import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
//import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

// main data record class with 6 measurement metrics generated randomly
public class VrMeasurement {

	private static final float deviation = 0.1f;
	Random rand = new Random();	

    private final static ObjectMapper JSON = new ObjectMapper();
    static {
        JSON.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private String resource;
    private String timestamp;
    private String host;
    private Float engagement;
    private Float focus;
    private Float excitement;
    private Float frustration;
    private Float stress;
    private Float relaxation;
    
    public VrMeasurement() {
        
        Date date = new Date();
        date.setTime(System.currentTimeMillis());
        
        this.resource = "EEG sensor";
        this.timestamp = toISO8601UTC(date);
        this.host = "user1";    	
    	this.engagement = getRandomFloat(0.9f);
        this.focus = getRandomFloat(0.8f);
        this.excitement = getRandomFloat(0.7f);
        this.frustration = getRandomFloat(0.3f);
        this.stress = getRandomFloat(0.2f);
        this.relaxation = getRandomFloat(0.5f);        
    }
     
    public String getResource() {
        return resource;
    }
   
    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getTimeStamp() {
        return timestamp;
    }

    public void setTimeStamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Float getEngagement() {
        return engagement;
    }

    public void setEngagement(Float engagement) {
        this.engagement = engagement;
    }
    
    public Float getFocus() {
        return focus;
    }

    public void setFocus(Float focus) {
        this.focus = focus;
    }
    
    public Float getExcitement() {
        return excitement;
    }

    public void setExcitement(Float excitement) {
        this.excitement = excitement;
    }
    
    public Float getFrustration() {
        return frustration;
    }

    public void setFrustration(Float frustration) {
        this.frustration = frustration;
    }
    
    public Float getStress() {
        return stress;
    }

    public void setStress(Float stress) {
        this.stress = stress;
    }
   
    public Float getRelaxation() {
        return relaxation;
    }

    public void setRelaxation(Float relaxation) {
        this.relaxation = relaxation;
    }
    
    public byte[] toJsonAsBytes() {
        try {
            return JSON.writeValueAsBytes(this);
        } catch (IOException e) {
            return null;
        }
    }
    
    public static VrMeasurement fromJsonAsBytes(byte[] bytes) {
        try {
            return JSON.readValue(bytes, VrMeasurement.class);
        } catch (IOException e) {
            return null;
        }
    }
    
    public Float getRandomFloat(Float mean) {
        
    	// set the measurement values using the deviation and provided mean        
    	Float max = mean + deviation/3;
    	Float min = mean - deviation/3;

        // randomly generate a value for each measurement
        Float value = rand.nextFloat() * (max - min) + min; 
        return value;
    }
    
    public static String toISO8601UTC(Date date) {
  	  TimeZone tz = TimeZone.getTimeZone("UTC");
  	  DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
  	  df.setTimeZone(tz);
  	  return df.format(date);
  	}
    
    @Override
    public String toString() {
        return String.format("Current measurement of values: %s %s %s %.3f %.3f %.3f %.3f %.3f %.3f ",
               resource, timestamp, host, engagement, focus, 
               excitement, frustration, stress, relaxation);
    }
}
