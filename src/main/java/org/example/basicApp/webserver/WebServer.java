/*
*
* Developed and adpated by Songjie Wang
* Department of EECS
* University of Missouri
*
*/

package org.example.basicApp.webserver;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import org.example.basicApp.utils.DynamoDBUtils;
import org.example.basicApp.utils.SampleUtils;
import org.example.basicApp.webserver.GetMeasurementServlet;

/**
 * Create an embedded HTTP server that responds with counts on the provided port.
 */
public class WebServer {
    /**
     * Start an embedded web server.
     * 
     * @param args Expecting 4 arguments: Port number, File path to static content, the name of the
     *        DynamoDB table where counts are persisted to, and the AWS region in which these resources
     *        exist or should be created.
     * @throws Exception Error starting the web server.
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println("Usage: " + WebServer.class
                    + " <port number> <directory for static content> <DynamoDB table name> <region>");
            System.exit(1);
        }
        Server server = new Server(Integer.parseInt(args[0]));
        String wwwroot = args[1];
        String countsTableName = args[2];
        Region region = SampleUtils.parseRegion(args[3]);

        // Servlet context
        ServletContextHandler context =
                new ServletContextHandler(ServletContextHandler.NO_SESSIONS | ServletContextHandler.NO_SECURITY);
        context.setContextPath("/api");

        // Static resource context
        ResourceHandler resources = new ResourceHandler();
        resources.setDirectoriesListed(false);
        resources.setWelcomeFiles(new String[] { "overview.html" });
        resources.setResourceBase(wwwroot);

        // Create the servlet to handle records
        AWSCredentialsProvider credentialsProvider = new DefaultAWSCredentialsProviderChain();
        ClientConfiguration clientConfig = SampleUtils.configureUserAgentForSample(new ClientConfiguration());
        AmazonDynamoDB dynamoDB = new AmazonDynamoDBClient(credentialsProvider, clientConfig);
        dynamoDB.setRegion(region);
        DynamoDBUtils dynamoDBUtils = new DynamoDBUtils(dynamoDB);
        context.addServlet(new ServletHolder(new GetMeasurementServlet(dynamoDBUtils.createMapperForTable(countsTableName))),
                "/GetMeasurements/*");
                
        HandlerList handlers = new HandlerList();
        handlers.addHandler(context);
        handlers.addHandler(resources);
        handlers.addHandler(new DefaultHandler());

        server.setHandler(handlers);
        server.start();
        server.join();
    }
}
