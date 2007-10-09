/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.file;

import org.mule.RegistryContext;
import org.mule.impl.RequestContext;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.FileUtils;

import java.io.File;

public class AutoDeleteOnFileDispatcherReceiverTestCase extends AbstractMuleTestCase
{

    private File validMessage;
    private String tempDirName = "input";
    File tempDir;
    UMOConnector connector;

    public void testAutoDeleteFalseOnDispatcher() throws Exception
    {
        ((FileConnector)connector).setAutoDelete(false);

        UMOEvent event = getTestEvent("TestData");
        event = RequestContext.setEvent(event);

        UMOMessage message = RequestContext.getEventContext().receiveEvent(getTestEndpointURI()+"/"+tempDirName+"?connector=FileConnector", 50000);
        assertNotNull(message.getPayload());

        File[] files = tempDir.listFiles();
        assertTrue(files.length > 0);
        for (int i = 0; i < files.length; i++)
        {
            assertTrue(files[i].getName().equals(message.getProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME)));
            files[i].delete();
        }
    }

    public void testAutoDeleteTrueOnDispatcher() throws Exception
    {
        ((FileConnector)connector).setAutoDelete(true);

        UMOEvent event = getTestEvent("TestData");
        event = RequestContext.setEvent(event);

        UMOMessage message = RequestContext.getEventContext().receiveEvent(getTestEndpointURI()+"/"+tempDirName, 50000);
        assertNotNull(message.getPayload());

        File[] files = tempDir.listFiles();
        assertTrue(files.length == 0);
    }

    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        // The working directory is deleted on tearDown
        tempDir = FileUtils.newFile(RegistryContext.getConfiguration().getWorkingDirectory(), tempDirName);
        if (!tempDir.exists())
        {
            tempDir.mkdirs();
        }
        validMessage = File.createTempFile("hello", ".txt", tempDir);
        assertNotNull(validMessage);
        connector = getConnector();
    }

    protected void doTearDown() throws Exception
    {
        // TestConnector dispatches events via the test: protocol to test://test
        // endpoints, which seems to end up in a directory called "test" :(
        FileUtils.deleteTree(FileUtils.newFile(getTestConnector().getProtocol()));
        super.doTearDown();
    }

    public UMOConnector getConnector() throws Exception {
        UMOConnector connector = new FileConnector();
        connector.setName("FileConnector");
        connector.setManagementContext(managementContext);
        managementContext.applyLifecycle(connector);
        managementContext.getRegistry().registerConnector(connector, managementContext);
        return connector;
    }

    public String getTestEndpointURI()
    {
        return "file://" + managementContext.getRegistry().getConfiguration().getWorkingDirectory();
    }
}