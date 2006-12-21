/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.file;

import org.mule.MuleManager;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.functional.FunctionalTestNotification;
import org.mule.tck.functional.FunctionalTestNotificationListener;
import org.mule.umo.UMOEventContext;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.util.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class FileFunctionalTestCase extends FunctionalTestCase implements FunctionalTestNotificationListener
{
    private Object receivedData = null;

    protected void doPostFunctionalSetUp() throws Exception
    {
        super.doPostFunctionalSetUp();
        super.setDisposeManagerPerSuite(true);
        MuleManager.getInstance().registerListener(this);
    }

    protected void doFunctionalTearDown() throws Exception
    {
        MuleManager.getInstance().unregisterListener(this);
        super.doFunctionalTearDown();
    }

    protected String getConfigResources()
    {
        return "org/mule/test/integration/providers/file/file-config.xml";
    }

    public void testRelative() throws FileNotFoundException, IOException, InterruptedException
    {
        // create binary file data to be written
        byte[] data = new byte[100000];
        for (int i = 0; i < data.length; i++)
        {
            data[i] = (byte)(Math.random() * 128);
        }

        File f = new File("./test/testfile.temp");
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f);
        IOUtils.write(data, fos);
        IOUtils.closeQuietly(fos);

        // atomically rename the file to make it available for polling
        f.renameTo(new File(f.getPath().replaceAll(".temp", ".data")));

        // give polling a chance
        Thread.sleep(5000);

        synchronized (this)
        {
            assertNotNull(receivedData);
            assertTrue(receivedData instanceof byte[]);
            byte[] receivedBytes = (byte[])receivedData;
            assertEquals(data.length, receivedBytes.length);
            assertTrue(Arrays.equals(data, receivedBytes));
        }
    }

    public void onNotification(UMOServerNotification notification)
    {
        synchronized (this)
        {
            logger.debug("received notification: " + notification);
            // save the received message data for verification
            this.receivedData = ((FunctionalTestNotification)notification).getReplyMessage();
        }
    }

    public static class FileTestComponent extends FunctionalTestComponent
    {
        public Object onCall(UMOEventContext context) throws Exception
        {
            // there should not be any transformers configured by default, so the
            // return message should be a byte[]
            super.setReturnMessage(context.getTransformedMessage());
            return super.onCall(context);
        }
    }

}
