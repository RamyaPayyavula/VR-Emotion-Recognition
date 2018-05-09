/*
*
* Developed and adpated by Songjie Wang
* Department of EECS
* University of Missouri
*
*/


package org.example.basicApp.webserver;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;

import org.example.basicApp.model.DdbRecordToRead;
import org.example.basicApp.model.DdbRecordToWrite;
import org.example.basicApp.model.SingleMeasurementValue;
import org.example.basicApp.model.VrMeasurement;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A servlet to serve requests for measurement values.
 */
@SuppressWarnings("serial")
public class GetMeasurementServlet extends HttpServlet {

    private static final Log LOG = LogFactory.getLog(GetMeasurementServlet.class);

    private static final ThreadLocal<DateFormat> DATE_FORMATTER = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            // ISO-8601 format
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            df.setTimeZone(UTC);
            return df;
        }
    };
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    // This is not serializable and we're not implementing safeguards to support it. Jetty is highly unlikely to
    // serialize this servlet anyway.
    private transient ObjectMapper JSON = new ObjectMapper();

    // This is not serializable and we're not implementing safeguards to support it. Jetty is highly unlikely to
    // serialize this servlet anyway.
    private transient DynamoDBMapper mapper;

    private static final String PARAMETER_RESOURCE = "resource";
    private static final String PARAMETER_RANGE_IN_SECONDS = "range_in_seconds";

    public GetMeasurementServlet(DynamoDBMapper mapper) {
        if (mapper == null) {
            throw new NullPointerException("DynamoDBMapper must not be null");
        }
        this.mapper = mapper;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        MultiMap<String> params = new MultiMap<>();
        UrlEncoded.decodeTo(req.getQueryString(), params, "UTF-8");

        //LOG.info(String.format("params include: %s \n", params.toString()));
        
        // We need both parameters to properly query for counts
        if (!params.containsKey(PARAMETER_RESOURCE) || !params.containsKey(PARAMETER_RANGE_IN_SECONDS)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        // Parse query string as a single integer - the number of seconds since "now" to query for new counts
        String resource = params.getString(PARAMETER_RESOURCE);
        int rangeInSeconds = Integer.parseInt(params.getString(PARAMETER_RANGE_IN_SECONDS));

        Calendar c = Calendar.getInstance();
        c.add(Calendar.SECOND, -1 * rangeInSeconds);
        Date startTime = c.getTime();
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Querying for measurements %s since %s", resource, DATE_FORMATTER.get().format(startTime)));
        }

        DynamoDBQueryExpression<DdbRecordToRead> query = new DynamoDBQueryExpression<>();
        DdbRecordToRead hashKey = new DdbRecordToRead();
        hashKey.setResource(resource);
        query.setHashKeyValues(hashKey);

        Condition recentUpdates =
                new Condition().withComparisonOperator(ComparisonOperator.GT)
                        .withAttributeValueList(new AttributeValue().withS(DATE_FORMATTER.get().format(startTime)));
        query.setRangeKeyConditions(Collections.singletonMap("timestamp", recentUpdates));

        List<DdbRecordToRead> queryRecords = mapper.query(DdbRecordToRead.class, query);
        
        //for debugging purpose only
        //DdbRecordToRead lastElement = queryRecords.iterator().next();        
        //LOG.info(String.format("record read from DynamoDB is: %s \n", lastElement.getValues().toString()));
        
        
        // Return the values as JSON
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_OK);
        JSON.writeValue(resp.getWriter(), queryRecords);
        
        //LOG.info(String.format("record include: %s \n", JSON.toString()));

    }
}
