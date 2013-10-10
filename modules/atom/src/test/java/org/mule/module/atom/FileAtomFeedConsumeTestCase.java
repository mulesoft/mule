/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    protected String getConfigResources()
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
