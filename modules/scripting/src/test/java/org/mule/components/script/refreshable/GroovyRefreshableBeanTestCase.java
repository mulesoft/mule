/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.components.script.refreshable;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.io.FileWriter;
import java.io.IOException;

public class GroovyRefreshableBeanTestCase extends FunctionalTestCase
{
    protected static final String scriptPath = "./target/test-classes/groovy-dynamic-script.groovy";
    protected static final String script1 = "import org.mule.umo.UMOEventContext; import org.mule.umo.lifecycle.Callable; public class GroovyDynamicScript implements Callable { public Object onCall(UMOEventContext eventContext) throws Exception{ return eventContext.getMessage().getPayloadAsString() + \" Received\"; }}";
    protected static final String script2 = script1.replaceAll(" Received", " Received2");
    protected static final String script3 = "public class GroovyDynamicScript { public String receive(String src) { return src + \" Received\"; }}";
    protected static final String script4 = script3.replaceAll(" Received", " Received2");
    protected static final int waitTime = 1000;
    
    protected String getConfigResources()
    {
        return "groovy-refreshable-config.xml";
    }
    
    private void writeScript(String src) throws IOException, InterruptedException
    {
        for (int i=0; i<1; i++)
        {
            FileWriter scriptFile = new FileWriter(scriptPath, false);
            scriptFile.write(src);
            scriptFile.flush();
            scriptFile.close();
        }
    }

    public void testFirstOnCallRefresh() throws Exception
    {
        writeScript(script1);
        Thread.sleep(waitTime); // wait for bean to refresh
        MuleClient client = new MuleClient();
        UMOMessage m = client.send("vm://groovy_refresh", "Test:", null);
        assertNotNull(m);
        assertEquals("Test: Received", m.getPayloadAsString());
    }
    
    public void testCallFirstTest() throws Exception
    {
        testFirstOnCallRefresh();
    }
    
    public void testSecondOnCallRefresh() throws Exception
    {
        writeScript(script2);
        Thread.sleep(waitTime); // wait for bean to refresh
        MuleClient client = new MuleClient();
        UMOMessage m = client.send("vm://groovy_refresh", "Test:", null);
        assertNotNull(m);
        assertEquals("Test: Received2", m.getPayloadAsString());
    }
    
    public void testFirstPojoRefresh() throws Exception
    {
        writeScript(script3);
        Thread.sleep(waitTime); // wait for bean to refresh
        MuleClient client = new MuleClient();
        UMOMessage m = client.send("vm://groovy_refresh", "Test:", null);
        assertNotNull(m);
        assertEquals("Test: Received", m.getPayloadAsString());
    }
    
    public void testSecondPojoRefresh() throws Exception
    {
        writeScript(script4);
        Thread.sleep(waitTime); // wait for bean to refresh
        MuleClient client = new MuleClient();
        UMOMessage m = client.send("vm://groovy_refresh", "Test:", null);
        assertNotNull(m);
        assertEquals("Test: Received2", m.getPayloadAsString());
    }

}



