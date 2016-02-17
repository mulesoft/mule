/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.PropertyScope;
import org.mule.construct.Flow;
import org.mule.functional.junit4.FunctionalTestCase;

import org.junit.Test;

public class SessionPropertiesTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/properties/session-properties-config.xml";
    }

    @Test
    public void setSessionPropertyUsingAPIGetInFlow() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestFlow());

        message.setProperty("key", "value", PropertyScope.SESSION);

        Flow flowA = (Flow) muleContext.getRegistry().lookupFlowConstruct("A");
        MuleEvent result = flowA.process(event);

        assertEquals("value", result.getMessageAsString());
    }

    @Test
    public void setSessionPropertyInFlowGetUsingAPI() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestFlow());

        Flow flowA = (Flow) muleContext.getRegistry().lookupFlowConstruct("B");
        MuleEvent result = flowA.process(event);

        assertEquals("value", result.getMessage().getProperty("key", PropertyScope.SESSION));
    }

    /**
     * When invoking a Flow directly session properties are preserved
     */
    @Test
    public void flowRefSessionPropertyPropagation() throws Exception
    {

        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestFlow());

        Object nonSerializable = new Object();
        message.setProperty("keyNonSerializable", nonSerializable, PropertyScope.SESSION);
        message.setProperty("key", "value", PropertyScope.SESSION);

        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("FlowRefWithSessionProperties");
        MuleEvent result = flow.process(event);

        assertSame(event.getSession(), result.getSession());

        assertNotNull(result);
        assertEquals("value", result.getMessage().getProperty("key", PropertyScope.SESSION));
        assertEquals("value1", result.getMessage().getProperty("key1", PropertyScope.SESSION));
        assertEquals("value2", result.getMessage().getProperty("key2", PropertyScope.SESSION));
        assertEquals(nonSerializable,
                     result.getMessage().getProperty("keyNonSerializable", PropertyScope.SESSION));
    }



    @Test
    public void defaultExceptionStrategy() throws Exception
    {
        flowRunner("defaultExceptionStrategy").asynchronously().run();
    }

    @Test
    public void catchExceptionStrategy() throws Exception
    {
        flowRunner("catchExceptionStrategy").asynchronously().run();
    }

}
