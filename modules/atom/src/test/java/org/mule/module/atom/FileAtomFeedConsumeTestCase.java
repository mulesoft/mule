/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom;

import org.mule.module.atom.event.EntryReceiver;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import java.io.File;
import java.io.FileOutputStream;

import org.junit.Test;

public class FileAtomFeedConsumeTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "file-atom-consume.xml";
    }

    @Test
    public void testConsumeFeedEntries() throws Exception
    {
        FileOutputStream fos = new FileOutputStream(new File(muleContext.getConfiguration().getWorkingDirectory(), "sample-feed.atom"));
        String feed = loadResourceAsString("sample-feed.atom");
        fos.write(feed.getBytes("UTF-8"));
        fos.close();

        final EntryReceiver component = (EntryReceiver)getComponent("feedSplitterConsumer");

        PollingProber prober = new PollingProber(10000, 100);
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return component.getCount() == 25;
            }

            @Override
            public String describeFailure()
            {
                return "Component did not process the expected number of feeds";
            }
        });
    }
}
