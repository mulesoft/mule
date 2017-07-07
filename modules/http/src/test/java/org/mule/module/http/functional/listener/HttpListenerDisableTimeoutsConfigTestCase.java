/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import org.junit.Rule;
import org.junit.Test;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.config.MuleProperties;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class HttpListenerDisableTimeoutsConfigTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port");

    @Rule
    public SystemProperty disableTimeouts = new SystemProperty(MuleProperties.SYSTEM_PROPERTY_PREFIX + "timeout.disable", "true");

    @Override
    protected String getConfigFile()
    {
        return "disable-timeouts-config.xml";
    }

    @Test
    public void httpListenerResponseTimeout() throws Exception
    {
        final MuleEvent muleEvent = runFlow("httpFlow", new DefaultMuleMessage("hi", muleContext));
        assertThat(muleEvent.getMessageAsString(), is("hi folks"));
    }
}