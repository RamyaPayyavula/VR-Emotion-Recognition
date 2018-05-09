/*
*
* Developed and adpated by Songjie Wang
* Department of EECS
* University of Missouri
*
*/

package org.example.basicApp.client;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;


// Generates MeasurementRecordProcessors
public class MeasurementRecordProcessorFactory implements IRecordProcessorFactory {

    private DynamoDBMeasurementWriter dbWriter;

    // Creates a new factory that uses the default configuration values for each processor it creates.
    public MeasurementRecordProcessorFactory(DynamoDBMeasurementWriter dbWriter) {
       
        if (dbWriter == null) {
            throw new NullPointerException("dbWriter must not be null");
        }
        this.dbWriter = dbWriter;

    }
    
    @Override
    public IRecordProcessor createProcessor() {
        return new MeasurementRecordProcessor(dbWriter);
    }
}
