/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.util;

import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WebServiceOnlineCheck
{
    public static final String TEST_URL = "http://www.webservicex.net/stockquote.asmx/GetQuote?symbol=CSCO";
    private static final Log logger = LogFactory.getLog(WebServiceOnlineCheck.class);

    public static boolean isWebServiceOnline()
    {
        logger.debug("Verifying that the web service is on-line...");
        
        BufferedReader input = null;
        try 
        {
            URLConnection conn = new URL(TEST_URL).openConnection();
            // setting these timeouts ensures the client does not deadlock indefinitely
            // when the server has problems.
            conn.setConnectTimeout(AbstractMuleContextTestCase.RECEIVE_TIMEOUT);
            conn.setReadTimeout(AbstractMuleContextTestCase.RECEIVE_TIMEOUT);
            InputStream in = conn.getInputStream();

            input = new BufferedReader(new InputStreamReader(in));            

            String response = "";
            String line;
            while ((line = input.readLine()) != null) 
            {
                response += line;
            }

            if (StringUtils.containsIgnoreCase(response, "Cisco"))
            {
                return true;
            }
            else
            {
                logger.warn("Unexpected response, web service does not seem to be on-line: \n" + response);
                return false;
            }
        } 
        catch (Exception e) 
        {
            logger.warn("Exception occurred, web service does not seem to be on-line: " + e);
            return false;
        } 
        finally
        {
            IOUtils.closeQuietly(input);
        }
    }
}


