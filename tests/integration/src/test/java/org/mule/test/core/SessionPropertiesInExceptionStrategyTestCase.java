/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.api.message.NullPayload;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class SessionPropertiesInExceptionStrategyTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/properties/session-properties-in-exception-strategy-config.xml";
    }

    @Test
    public void sessionPropertyIsNotLost() throws Exception
    {
        List<String> list = new ArrayList<String>();
        list.add("one");
        list.add("two");
        list.add("three");

        MuleEvent event = flowRunner("test").withPayload(list).run();

        assertNull(event.getMessage().getExceptionPayload());
        assertFalse(event.getMessage().getPayload() instanceof NullPayload);
        assertThat(event.getSession().getProperty("ErrorCount"), is(list.size()));
    }
}
