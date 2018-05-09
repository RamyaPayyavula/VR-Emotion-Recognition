/*
*
* Developed by Songjie Wang
* Department of EECS
* University of Missouri
*
*/


package org.example.basicApp.ddb;


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.JsonMarshaller;
import org.example.basicApp.model.SingleMeasurementValue;

/**
 * Marshall {@link SingleMeasurementValue}s as JSON strings when using the {@link DynamoDBMapper}.
 */
public class MeasurementRecordMarshaller extends JsonMarshaller<SingleMeasurementValue> {
}
