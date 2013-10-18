/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.construct;

import org.mule.api.MuleEvent;
import org.mule.component.AbstractComponent;
import org.mule.construct.SimpleService.Type;
import org.mule.tck.MuleTestUtils;
import org.mule.util.StringUtils;

import java.util.Collections;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SimpleServiceTestCase extends AbstractFlowConstuctTestCase
{
    private SimpleService simpleService;

    @SuppressWarnings("unchecked")
    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        simpleService = new SimpleService("test-simple-service", muleContext, directInboundMessageSource,
            Collections.EMPTY_LIST, Collections.EMPTY_LIST, new StringReverserComponent(), Type.DIRECT);
    }

    @Override
    protected AbstractFlowConstruct getFlowConstruct()
    {
        return simpleService;
    }

    @Test
    public void testProcess() throws Exception
    {
        simpleService.initialise();
        simpleService.start();
        MuleEvent response = directInboundMessageSource.process(MuleTestUtils.getTestEvent("hello",
            muleContext));

        assertEquals("olleh", response.getMessageAsString());
    }

    private static class StringReverserComponent extends AbstractComponent
    {
        @Override
        protected Object doInvoke(MuleEvent event) throws Exception
        {
            return StringUtils.reverse(event.getMessageAsString());
        }
    }
}
