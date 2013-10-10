/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
