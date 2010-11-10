/*
 * $Id:  $
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.rss;

import org.mule.tck.FunctionalTestCase;

import java.io.File;
import java.io.FileOutputStream;

public class FileRssFeedConsumeTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "file-rss-consume.xml";
    }

    public void testConsumeFeedEntries() throws Exception
    {
        FileOutputStream fos = new FileOutputStream(new File(muleContext.getConfiguration().getWorkingDirectory(), "sample-feed.rss"));
        String feed = loadResourceAsString("sample-feed.rss");
        fos.write(feed.getBytes());
        fos.close();

        Thread.sleep(3000);
        EntryReceiver component = (EntryReceiver) getComponent("feedSplitterConsumer");
        assertEquals(25, component.getCount());
    }

}
