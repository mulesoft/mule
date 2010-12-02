/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet.jetty;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.client.DefaultLocalMuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.HttpConnector;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

public class WebappsTestCase extends FunctionalTestCase
{

    public WebappsTestCase() throws Exception
    {
        File webapps = new File("target/webapps");
        FileUtils.deleteDirectory(webapps);
        webapps.mkdir();
        
        FileUtils.copyFile(new File("src/test/resources/test.war"), new File(webapps, "test.war"));
    }
    
    public void testWebapps() throws Exception
    {
        Map<String,Object> props = new HashMap<String,Object>();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, "GET");
        
        DefaultLocalMuleClient client = new DefaultLocalMuleClient(muleContext);
        MuleMessage result = client.send("http://localhost:63081/test/hello", 
            new DefaultMuleMessage("", muleContext),
            props);
        
        assertEquals("Hello", result.getPayloadAsString());
    }
    
    @Override
    protected String getConfigResources()
    {
        return "jetty-webapps.xml";
    }


}
