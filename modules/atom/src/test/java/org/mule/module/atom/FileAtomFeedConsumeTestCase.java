/*
 * $Id:  $
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom;

import org.mule.module.atom.event.EntryReceiver;
import org.mule.tck.FunctionalTestCase;

import java.io.File;
import java.io.FileOutputStream;

public class FileAtomFeedConsumeTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "file-atom-consume.xml";
    }

    public void testConsumeFeedEntries() throws Exception
    {
        FileOutputStream fos = new FileOutputStream(new File(muleContext.getConfiguration().getWorkingDirectory(), "sample-feed.atom"));
        String feed = loadResourceAsString("sample-feed.atom");
        fos.write(feed.getBytes());

        //allow the file connector to poll a couple of times to ensure we only get the same 25 entries
        Thread.sleep(3000);
        EntryReceiver component = (EntryReceiver)getComponent("feedSplitterConsumer");
        assertEquals(25, component.getCount());
    }


}
