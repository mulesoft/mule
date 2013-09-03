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

import org.junit.BeforeClass;
import org.junit.Test;


public class PollScheduleDelayTestCase extends FunctionalTestCase
{

    private static List<String> negative = new ArrayList<String>();
    private static List<String> zero = new ArrayList<String>();
    private static List<String> positive = new ArrayList<String>();

    @BeforeClass
    public static void setProperties()
    {
        System.setProperty("frequency.millis", "100");
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/schedule/poll-scheduler-delay-config.xml";
    }

    @Test
    public void test() throws Exception
    {
        checkForNegativeCollectionToBeFilled();
        checkForZeroCollectionToBeFilled();
        checkForPositiveCollectionToBeEmpty(true);

        waitForPollElements();

        checkForNegativeCollectionToBeFilled();
        checkForZeroCollectionToBeFilled();
        checkForPositiveCollectionToBeEmpty(false);
    }

    private void waitForPollElements() throws InterruptedException
    {
        Thread.sleep(3000);
    }

    private void checkForNegativeCollectionToBeFilled()
    {
        synchronized (negative)
        {
            negative.size();
            assertTrue(negative.size() > 0);
            for (String s : negative)
            {
                assertEquals(s, "negative");
            }
        }
    }

    private void checkForZeroCollectionToBeFilled()
    {
        synchronized (zero)
        {
            zero.size();
            assertTrue(zero.size() > 0);
            for (String s : zero)
            {
                assertEquals(s, "zero");
            }
        }
    }

    private void checkForPositiveCollectionToBeEmpty(boolean isEmpty)
    {
        synchronized (positive)
        {
            positive.size();
            if (isEmpty)
            {
                assertEquals(0, positive.size());
            }
            else
            {
                assertTrue(positive.size() > 0);
            }
            for (String s : positive)
            {
                assertEquals(s, "positive");
            }
        }
    }


    public static class NegativeComponent
    {

        public boolean process(String s)
        {
            synchronized (negative)
            {

                if (negative.size() < 10)
                {
                    negative.add(s);
                    return true;
                }
            }
            return false;
        }
    }

    public static class ZeroComponent
    {

        public boolean process(String s)
        {
            synchronized (zero)
            {

                if (zero.size() < 10)
                {
                    zero.add(s);
                    return true;
                }
            }
            return false;
        }
    }

    public static class PositiveComponent
    {

        public boolean process(String s)
        {
            synchronized (positive)
            {

                if (positive.size() < 10)
                {
                    positive.add(s);
                    return true;
                }
            }
            return false;
        }
    }
}