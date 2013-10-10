/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.construct;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;

public class FlowUseCaseProcessingStrategyTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/construct/flow-usecase-processing-strategy-config.xml";
    }

    @Test
    public void testHTTPStatusCodeExceptionSyncStrategy() throws MuleException
    {
        MuleClient client = muleContext.getClient();
        MuleMessage exception = client.send("http://localhost:" + dynamicPort.getNumber(), null, null);

        assertEquals("500", exception.getInboundProperty("http.status", "0"));        
    }

    @Test    
    public void testFileAutoDeleteSyncStrategy() throws Exception
    {     
        MuleClient client = muleContext.getClient();    
        File tempFile = createTempFile("mule-file-test-sync-");        
        client.request("vm://exception", 5000);       
        
        assertTrue(tempFile.exists());                
    }
    
    @Test
    public void testFileAutoDeleteAsyncStrategy() throws Exception
    {  
        MuleClient client = muleContext.getClient();   
        File tempFile = createTempFile("mule-file-test-async-");
        client.request("vm://exception", 5000);
        
        assertFalse(tempFile.exists());              
    }
    
    private File createTempFile(String fileName) throws IOException
    {        
        File directory = new File("./.mule");
        File file = File.createTempFile(fileName, ".txt", directory);       
        file.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(file);
        IOUtils.write("The quick brown fox jumps over the lazy dog", fos);
        IOUtils.closeQuietly(fos);
        
        return file;
    }
         
}



