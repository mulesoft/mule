/*
 * $Id\$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.schedule;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.tck.junit4.FunctionalTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class PollScheduleNoFrequencySetTestCase extends FunctionalTestCase
{

    private static List<String> foo = new ArrayList<String>();

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/schedule/poll-scheduler-no-frequency-set-config.xml";
    }

    @Test
    public void test() throws Exception
    {
        checkForFooCollectionToBeFilled(0, 1);

        waitForPollElements();

        checkForFooCollectionToBeFilled(2, 3);

        waitForPollElements();

        checkForFooCollectionToBeFilled(4, 5);
    }

    private void waitForPollElements() throws InterruptedException
    {
        Thread.sleep(2000);
    }

    private void checkForFooCollectionToBeFilled(int min, int max)
    {
        synchronized (foo)
        {
            foo.size();
            assertTrue(foo.size() >= min && foo.size() <= max);
            for (String s : foo)
            {
                assertEquals(s, "foo");
            }
        }
    }

    public static class FooComponent
    {

        public boolean process(String s)
        {
            synchronized (foo)
            {

                if (foo.size() < 10)
                {
                    foo.add(s);
                    return true;
                }
            }
            return false;
        }
    }
}
