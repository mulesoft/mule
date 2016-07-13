/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.properties;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.functional.junit4.FlowRunner;
import org.mule.runtime.core.api.MuleEvent;

import org.junit.Test;

public class SessionPropertiesTestCase extends AbstractIntegrationTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/properties/session-properties-config.xml";
    }

    @Test
    public void setSessionPropertyUsingAPIGetInFlow() throws Exception
    {
        MuleEvent result = flowRunner("A").withPayload("data")
                                          .withSessionProperty("key", "value")
                                          .run();

        assertThat(result.getMessageAsString(), is("value"));
    }

    @Test
    public void setSessionPropertyInFlowGetUsingAPI() throws Exception
    {
        MuleEvent result = flowRunner("B").withPayload("data").run();
        assertThat(result.getSession().getProperty("key"), is("value"));
    }

    /**
     * When invoking a Flow directly session properties are preserved
     */
    @Test
    public void flowRefSessionPropertyPropagation() throws Exception
    {
        Object nonSerializable = new Object();

        FlowRunner runner = flowRunner("FlowRefWithSessionProperties").withPayload("data")
                                                                    .withSessionProperty("keyNonSerializable", nonSerializable)
                                                                    .withSessionProperty("key", "value");
        MuleEvent event = runner.buildEvent();
        MuleEvent result = runner.run();

        assertSame(event.getSession(), result.getSession());

        assertNotNull(result);
        assertThat(result.getSession().getProperty("key"), is("value"));
        assertThat(result.getSession().getProperty("key1"), is("value1"));
        assertThat(result.getSession().getProperty("key2"), is("value2"));
        assertThat(result.getSession().getProperty("keyNonSerializable"), is(nonSerializable));
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
