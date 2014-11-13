/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;


import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class HttpListenerCustomTlsConfigTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort port1 = new DynamicPort("port1");
    @Rule
    public DynamicPort port2 = new DynamicPort("port2");
    @Rule
    public DynamicPort port3 = new DynamicPort("port3");

    @Override
    protected String getConfigFile()
    {
        return "http-listener-custom-tls-config.xml";
    }

    @Test
    public void customTlsGlobalContext() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("testFlowGlobalContextClient");
        final MuleEvent res = flow.process(getTestEvent("data"));
        assertThat(res.getMessage().getPayloadAsString(), is("ok"));
    }

    @Test
    public void customTlsNestedContext() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("testFlowNestedContextClient");
        final MuleEvent res = flow.process(getTestEvent("data"));
        assertThat(res.getMessage().getPayloadAsString(), is("all right"));
    }

}
