/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.devkit.processor;

import static org.junit.Assert.assertTrue;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class DevkitBasedMessageProcessorTest
{

    @Test
    public void testThatProcessMethodCanBeOverridden() throws MuleException
    {
        DevkitMessageProcessorProxy connectorMessageProcessor = new DevkitMessageProcessorProxy("anyName");

        connectorMessageProcessor.process(null);

        assertTrue(connectorMessageProcessor.wasCalled);
    }

    private static class DevkitMessageProcessorProxy extends DevkitBasedMessageProcessor
    {

        boolean wasCalled;

        public DevkitMessageProcessorProxy(String operationName)
        {
            super(operationName);
        }

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            wasCalled = true;
            return event;
        }

        @Override
        protected MuleEvent doProcess(MuleEvent event) throws Exception
        {
            wasCalled = false;
            return event;
        }


    }
}
