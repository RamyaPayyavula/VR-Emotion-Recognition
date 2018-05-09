/*
*
* Developed and adpated by Songjie Wang
* Department of EECS
* University of Missouri
*
*/

package org.example.basicApp.utils;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;

// A collection of utilities for the Amazon Kinesis sample application.
public class SampleUtils {

    // Creates a new client configuration.     
    public static ClientConfiguration configureUserAgentForSample(ClientConfiguration clientConfig) {
        ClientConfiguration newConfig = new ClientConfiguration(clientConfig);
        StringBuilder userAgent = new StringBuilder(ClientConfiguration.DEFAULT_USER_AGENT);

        // Separate regions of the UserAgent with a space
        userAgent.append(" ");
        // Append the repository name followed by version number of the sample
        userAgent.append("visualApp/7.0");
        newConfig.setUserAgent(userAgent.toString());
        return newConfig;
    }

    // Creates an AWS Region 
    public static Region parseRegion(String regionStr) {
        Region region = RegionUtils.getRegion(regionStr);

        if (region == null) {
            System.err.println(regionStr + " is not a valid AWS region.");
            System.exit(1);
        }
        return region;
    }

}
