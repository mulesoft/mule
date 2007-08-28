/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.components.script.refreshable;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.io.FileWriter;
import java.io.IOException;

public abstract class AbstractRefreshableBeanTestCase extends FunctionalTestCase
{
    protected static String scriptPath_callable;
    protected static String scriptPath_bean;
    protected static String scriptPath_changeInterfaces;
    protected static String script1;
    protected static String script2;
    protected static String script3;
    protected static String script4;
    protected static int waitTime = 1000;
    
    protected void writeScript(String src, String path) throws IOException, InterruptedException
    {
        for (int i=0; i<1; i++)
        {
            FileWriter scriptFile = new FileWriter(path, false);
            scriptFile.write(src);
            scriptFile.flush();
            scriptFile.close();
        }
    }
    
    protected void runScriptTest(String script, String path, String endpoint, String payload, String expectedResult) throws Exception
    {
        writeScript(script, path);
        Thread.sleep(waitTime); // wait for bean to refresh
        MuleClient client = new MuleClient();
        UMOMessage m = client.send(endpoint, payload, null);
        assertNotNull(m);
        assertEquals(expectedResult, m.getPayloadAsString());
    }

}


