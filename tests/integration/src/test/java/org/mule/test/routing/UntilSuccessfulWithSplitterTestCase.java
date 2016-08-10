/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.processor.MessageProcessor;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class UntilSuccessfulWithSplitterTestCase extends AbstractIntegrationTestCase
{

    private static final int TIMEOUT = 5;
    private static Multiset<String> seenPayloads;
    private static CountDownLatch latch;

    @Override
    protected String getConfigFile()
    {
        return "until-successful-with-splitter.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        seenPayloads = HashMultiset.create();
        latch = new CountDownLatch(2);
    }

    @Test
    public void withSplitter() throws Exception
    {
        runFlow("withSplitter");

        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS), is(true));
        assertThat(seenPayloads.count("a"), is(2));
        assertThat(seenPayloads.count("b"), is(2));
    }

    public static class FailAtFirstAttempt implements MessageProcessor
    {

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            final String payload = event.getMessageAsString();
            seenPayloads.add(payload);

            if (seenPayloads.count(payload) == 1)
            {
                throw new RuntimeException("first time");
            }

            latch.countDown();

            return event;
        }
    }
}
