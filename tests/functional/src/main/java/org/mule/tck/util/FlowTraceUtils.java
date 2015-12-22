/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.util;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.notification.FlowCallStack;
import org.mule.api.context.notification.FlowStackElement;
import org.mule.api.processor.MessageProcessor;

import java.util.concurrent.CountDownLatch;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class FlowTraceUtils
{
    public static class FlowStackAsserter implements MessageProcessor
    {

        public static FlowCallStack stackToAssert;

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            stackToAssert = event.getFlowCallStack().clone();
            return event;
        }
    }

    public static class FlowStackAsyncAsserter extends FlowStackAsserter
    {

        public static CountDownLatch latch;

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            super.process(event);
            latch.countDown();
            return event;
        }
    }


    public static void assertStackElements(FlowCallStack flowStack, Matcher<FlowStackElement>... flowStackElementMatchers)
    {
        assertThat(flowStack.getElements(), hasSize(flowStackElementMatchers.length));
        int i = 0;
        for (Matcher<FlowStackElement> flowStackElementMatcher : flowStackElementMatchers)
        {
            assertThat(flowStack.getElements().get(i), flowStackElementMatcher);
            ++i;
        }
    }

    public static Matcher<FlowStackElement> isFlowStackElement(final String flowName, final String executingMessageProcessor)
    {
        return new TypeSafeMatcher<FlowStackElement>()
        {
            @Override
            protected boolean matchesSafely(FlowStackElement flowStackElement)
            {
                return flowStackElement.getFlowName().startsWith(flowName)
                       && (executingMessageProcessor == null ? flowStackElement.getProcessorPath() == null : flowStackElement.getProcessorPath().startsWith(executingMessageProcessor + " @"));
            }

            @Override
            public void describeTo(Description description)
            {
                if (executingMessageProcessor == null)
                {
                    description.appendText("<")
                               .appendText(flowName);
                }
                else
                {
                    description.appendText("<")
                               .appendText(flowName)
                               .appendText("(")
                               .appendText(executingMessageProcessor)
                               .appendText("*)");
                }
            }
        };
    }
}
