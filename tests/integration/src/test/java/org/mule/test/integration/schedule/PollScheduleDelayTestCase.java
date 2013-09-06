/*
 * $Id$
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
import static org.junit.Assert.fail;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

public class PollScheduleDelayTestCase extends FunctionalTestCase
{

    private static final List<String> negativeFlowResponses = new ArrayList<String>();
    private static final List<String> zeroFlowResponses = new ArrayList<String>();
    private static final List<String> positiveFlowResponses = new ArrayList<String>();

    @Rule
    public SystemProperty systemProperty = new SystemProperty("frequency.millis","100");

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/schedule/poll-scheduler-delay-config.xml";
    }

    @Test
    public void testPollStartDelay() throws Exception
    {
        waitForPollElements();

        assertCollectionToBeFilledWithContent(negativeFlowResponses, "negative");
        assertCollectionToBeFilledWithContent(zeroFlowResponses, "zero");
        try{
            assertCollectionToBeFilledWithContent(positiveFlowResponses, "positive");
            fail("Collection wasn't empty");
        }catch(AssertionError error){
            assertEquals("java.lang.AssertionError", error.toString());
        }

        waitForPollElements();

        assertCollectionToBeFilledWithContent(negativeFlowResponses, "negative");
        assertCollectionToBeFilledWithContent(zeroFlowResponses, "zero");
        assertCollectionToBeFilledWithContent(positiveFlowResponses, "positive");
    }

    private void waitForPollElements() throws InterruptedException
    {
        Thread.sleep(300);
    }

    private void assertCollectionToBeFilledWithContent(List<String> collection, String expectedContent)
    {
        synchronized (collection)
        {
            assertTrue(collection.size() > 0);
            for (String s : collection)
            {
                assertEquals(s, expectedContent);
            }
        }
    }

    public static class NegativeComponent extends ComponentProcessor
    {

        public NegativeComponent()
        {
            this.myCollection = negativeFlowResponses;
        }
    }

    public static class ZeroComponent extends ComponentProcessor
    {

        public ZeroComponent()
        {
            this.myCollection = zeroFlowResponses;
        }
    }

    public static class PositiveComponent extends ComponentProcessor
    {

        public PositiveComponent()
        {
            this.myCollection = positiveFlowResponses;
        }
    }
}