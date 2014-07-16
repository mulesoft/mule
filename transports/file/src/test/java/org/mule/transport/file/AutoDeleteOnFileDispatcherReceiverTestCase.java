/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transport.Connector;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.InputStream;

import org.junit.Test;

public class AutoDeleteOnFileDispatcherReceiverTestCase extends AbstractMuleContextTestCase
{

    private File validMessage;
    private String tempDirName = "input";
    File tempDir;
    Connector connector;

    @Test
    public void testAutoDeleteFalseOnDispatcher() throws Exception
    {
        ((FileConnector)connector).setAutoDelete(false);

        MuleEvent event = getTestEvent("TestData");
        event = RequestContext.setEvent(event);

        MuleMessage message = RequestContext.getEventContext().requestEvent(getTestEndpointURI()+"/"+tempDirName+"?connector=FileConnector", 50000);
        // read the payload into a string so the file is deleted on InputStream.close()
        assertNotNull(message.getPayloadAsString());

        File[] files = tempDir.listFiles();
        assertTrue(files.length > 0);
        for (int i = 0; i < files.length; i++)
        {
            assertTrue(files[i].getName().equals(message.getInboundProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME)));
            files[i].delete();
        }
    }

    @Test
    public void testAutoDeleteTrueOnDispatcher() throws Exception
    {
        ((FileConnector)connector).setAutoDelete(true);

        MuleEvent event = getTestEvent("TestData");
        event = RequestContext.setEvent(event);

        MuleMessage message = RequestContext.getEventContext().requestEvent(getTestEndpointURI()+"/"+tempDirName, 50000);
        assertNotNull(message.getPayload());
        assertTrue(message.getPayload() instanceof InputStream);

        // Auto-delete happens after FileInputStream.close() when streaming.  Streaming is default.
        assertTrue(tempDir.listFiles().length > 0);
        ((InputStream) message.getPayload()).close();
        // Give file-system some time (annoying but necessary wait apparently due to OS caching?)
        Prober prober = new PollingProber(1000, 100);
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return tempDir.listFiles().length == 0;
            }

            @Override
            public String describeFailure()
            {
                return "File was not deleted from temp directory";
            }
        });
        assertTrue(tempDir.listFiles().length == 0);
        
        
    }

    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        // The working directory is deleted on tearDown
        tempDir = FileUtils.newFile(muleContext.getConfiguration().getWorkingDirectory(), tempDirName);
        tempDir.deleteOnExit();
        if (!tempDir.exists())
        {
            tempDir.mkdirs();
        }
        validMessage = File.createTempFile("hello", ".txt", tempDir);
        assertNotNull(validMessage);
        connector = getConnector();
        connector.start();
    }

    protected void doTearDown() throws Exception
    {
        // TestConnector dispatches events via the test: protocol to test://test
        // endpoints, which seems to end up in a directory called "test" :(
        FileUtils.deleteTree(FileUtils.newFile(getTestConnector().getProtocol()));
        super.doTearDown();
    }

    public Connector getConnector() throws Exception {
        Connector connector = new FileConnector(muleContext);
        connector.setName("FileConnector");
        muleContext.getRegistry().registerConnector(connector);
        return connector;
    }

    public String getTestEndpointURI()
    {
        return "file://" + muleContext.getConfiguration().getWorkingDirectory();
    }
}
