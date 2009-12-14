/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.messaging.meps;

import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.HttpConstants;
import org.mule.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MessagePropertiesPropagationTestCase extends FunctionalTestCase
{
    public static final String TEST_URL = "http://www.webservicex.net/stockquote.asmx/GetQuote?symbol=CSCO";

    public MessagePropertiesPropagationTestCase()
    {
        super();
        
        // Do not fail test case upon timeout because this probably just means
        // that the 3rd-party web service is off-line.
        setFailOnTimeout(false);
    }
    
    
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/messaging/meps/message-properties-propagation.xml";
    }

    /**
     * As per EE-1613, the Correlation-related properties _should_ be propagated to the response message by default.
     */
    public void testPropagatedPropertiesWithHttpTransport() throws Exception
    {
        MuleClient client = new MuleClient();
        Map<String, String> props = new HashMap<String, String>();
        props.put("Content-Type", "application/x-www-form-urlencoded");
        props.put(MuleProperties.MULE_CORRELATION_ID_PROPERTY, "TestID");
        props.put(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY, "TestGroupSize");
        props.put(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, "TestSequence");
        MuleMessage response = client.send("vm://httpService1", "symbol=IBM", props);
        assertNotNull(response);
        assertTrue(response.getPayloadAsString().contains("PreviousClose"));
        assertEquals("TestID", response.getProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY));
        assertEquals("TestGroupSize", response.getProperty(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY));
        assertEquals("TestSequence", response.getProperty(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY));
    }

    /**
     * As per EE-1613, the Correlation-related properties _should_ be propagated to the response message by default.
     */
    public void testPropagatedPropertiesWithCxfTransport() throws Exception
    {
        MuleClient client = new MuleClient();
        Map<String, String> props = new HashMap<String, String>();
        props.put(MuleProperties.MULE_CORRELATION_ID_PROPERTY, "TestID");
        props.put(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY, "TestGroupSize");
        props.put(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, "TestSequence");
        MuleMessage response = client.send("vm://cxfService1", "IBM", props);
        assertNotNull(response);
        assertTrue(response.getPayloadAsString().contains("PreviousClose"));
        assertEquals("TestID", response.getProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY));
        assertEquals("TestGroupSize", response.getProperty(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY));
        assertEquals("TestSequence", response.getProperty(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY));
    }

    /**
     * As per EE-1611/MULE-4302, custom properties and non-Correlation-related properties _should not_ be propagated 
     * to the response message by default.
     */
    public void testNotPropagatedPropertiesWithHttpTransport() throws Exception
    {
        MuleClient client = new MuleClient();
        Map<String, String> props = new HashMap<String, String>();
        props.put("Content-Type", "application/x-www-form-urlencoded");
        props.put("some", "thing");
        props.put("other", "stuff");
        props.put(HttpConstants.HEADER_CONTENT_TYPE, "text/bizarre;charset=utf-16");
        MuleMessage response = client.send("vm://httpService1", "symbol=IBM", props);
        assertNotNull(response);
        assertNull(response.getProperty("some"));
        assertNull(response.getProperty("other"));
        assertEquals("text/plain; charset=utf-8", response.getProperty(HttpConstants.HEADER_CONTENT_TYPE));
        assertEquals("utf-8", response.getEncoding());
    }

    /**
     * As per EE-1611/MULE-4302, custom properties and non-Correlation-related properties _should not_ be propagated 
     * to the response message by default.
     */
    public void testNotPropagatedPropertiesWithCxfTransport() throws Exception
    {
        MuleClient client = new MuleClient();
        Map<String, String> props = new HashMap<String, String>();
        props.put("some", "thing");
        props.put("other", "stuff");
        props.put(HttpConstants.HEADER_CONTENT_TYPE, "text/bizarre;charset=utf-16");
        MuleMessage response = client.send("vm://cxfService1", "IBM", props);
        assertNotNull(response);
        assertNull(response.getProperty("some"));
        assertNull(response.getProperty("other"));
        assertEquals("text/xml; charset=utf-8", response.getProperty(HttpConstants.HEADER_CONTENT_TYPE));
        assertEquals("utf-8", response.getEncoding());
    }

    /**
     * Force the properties to be propagated to the response message.
     */
    public void testForcePropagatedPropertiesWithHttpTransport() throws Exception
    {
        MuleClient client = new MuleClient();
        Map<String, String> props = new HashMap<String, String>();
        props.put("Content-Type", "application/x-www-form-urlencoded");
        props.put("some", "thing");
        props.put("other", "stuff");
        MuleMessage response = client.send("vm://httpService2", "symbol=IBM", props);
        assertNotNull(response);
        assertTrue(response.getPayloadAsString().contains("PreviousClose"));
        assertEquals("thing", response.getProperty("some"));
        assertEquals("stuff", response.getProperty("other"));
    }

    /**
     * Force the properties to be propagated to the response message.
     */
    public void testForcePropagatedPropertiesWithCxfTransport() throws Exception
    {
        MuleClient client = new MuleClient();
        Map<String, String> props = new HashMap<String, String>();
        props.put("some", "thing");
        props.put("other", "stuff");
        MuleMessage response = client.send("vm://cxfService2", "symbol=IBM", props);
        assertNotNull(response);
        assertTrue(response.getPayloadAsString().contains("PreviousClose"));
        assertEquals("thing", response.getProperty("some"));
        assertEquals("stuff", response.getProperty("other"));
    }
    //////////////////////////////////////////////////////////////////////////////////////////
    // TODO The following is a copy/paste from the StockQuote example, it could ideally share 
    // a common super-class so that the code is not duplicated here.
    //////////////////////////////////////////////////////////////////////////////////////////
    
    /**
     * If a simple call to the web service indicates that it is not responding properly,
     * we disable the test case so as to not report a test failure which has nothing to do
     * with Mule.
     */
    @Override
    protected boolean isDisabledInThisEnvironment()
    {
        return !isWebServiceOnline();
    }
    
    /**
     * @return true if the web service is functioning correctly
     */
    protected boolean isWebServiceOnline()
    {
        logger.debug("Verifying that the web service is on-line...");
        
        BufferedReader input = null;
        try 
        {
            input = new BufferedReader(new InputStreamReader(new URL(TEST_URL).openStream()));

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
            if (input != null)
            {
                try
                {
                    input.close();
                }
                catch (IOException ioe) {}
            }
        }
    }
}
