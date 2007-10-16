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

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;
import org.mule.util.FileUtils;

import java.io.File;

public class FileComparatorTestCase extends FunctionalTestCase
{

    public static final String PATH = "./.mule/in/";
    public static final String FILE1_NAME = "first";
    public static final String FILE2_NAME = "second";
    public static final String OUT_QUEUE = "vm://out";
    public static final String FILE_CONNECTOR_NAME = "fileConnector";
    public static final int TIMEOUT = 5000;


    public void testComparator() throws Exception
    {
        managementContext.getRegistry().lookupConnector(FILE_CONNECTOR_NAME).stop();
        File f1 = FileUtils.newFile(PATH + FILE1_NAME);
        f1.createNewFile();
        Thread.sleep(1);
        File f2 = FileUtils.newFile(PATH + FILE2_NAME);
        f2.createNewFile();
        Thread.sleep(10);
        managementContext.getRegistry().lookupConnector(FILE_CONNECTOR_NAME).start();

        MuleClient client = new MuleClient();
        UMOMessage message = client.receive(OUT_QUEUE, TIMEOUT);
        assertNotNull(message);
        assertEquals(message.getProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME), FILE2_NAME);
        message = client.receive(OUT_QUEUE, TIMEOUT);
        assertNotNull(message);
        assertEquals(message.getProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME), FILE1_NAME);
        client.dispose();

    }

    protected String getConfigResources()
    {
        return "file-functional-config.xml";
    }
}
