/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static org.junit.Assert.fail;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class PartialStartupTestCase extends AbstractIntegrationTestCase
{

    public PartialStartupTestCase()
    {
        super();
        setStartContext(false);
    }

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/components/partial-startup-test.xml";
    }

    @Test
    public void testStopAfterPartialStartup() throws Exception
    {
        final List<String> initialThreads = collectThreadNames();
        try
        {
            muleContext.start();
            fail("Expected Mule to fail to start, due to our RudeMessageProcessor");
        }
        catch (Exception e)
        {
            System.err.println("Expected Exception:");
            e.printStackTrace();
        }

        //Mule failed to start, so go ahead and dispose it(Mule will not let us call stop at this point)
        muleContext.dispose();

        new PollingProber(10000, 100).check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                List<String> currentThreads = collectThreadNames();
                return
                        countOcurrences(currentThreads, "SHUTDOWN_TEST_FLOW") == 0 &&
                        countOcurrences(initialThreads, "MuleServer") == countOcurrences(currentThreads, "MuleServer") + 1;
            }

            @Override
            public String describeFailure()
            {
                return "mule threads running during dispose";
            }
        });
    }

    private static List<String> collectThreadNames()
    {
        List<String> threadNames = new ArrayList<String>();
        for (Thread t : Thread.getAllStackTraces().keySet())
        {
            threadNames.add(t.getName());
        }
        return threadNames;
    }

    private static int countOcurrences(List<String> elements, String prefix)
    {
        int count = 0;
        if (elements != null)
        {
            for (String element : elements)
            {
                if (element.startsWith(prefix))
                {
                    count++;
                }
            }
        }
        return count;
    }
}
