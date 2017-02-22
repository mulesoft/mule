/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;


import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class HttpListenerTlsRevocationConfigTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort portAllowed = new DynamicPort("portAllowed");

    @Rule
    public DynamicPort portRevoked = new DynamicPort("portRevoked");

    @Override
    protected String getConfigFile()
    {
        return "http-listener-tls-revocation-config.xml";
    }

    @Test
    public void clientCertified() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("testFlowClient");
        final MuleEvent res = flow.process(getTestEvent("data"));
        assertThat(res.getMessage().getPayloadAsString(), is("ok"));
    }

    @Test(expected = MessagingException.class)
    public void clientNotCertified() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("testFlowClientNotCertified");
        final MuleEvent res = flow.process(getTestEvent("data"));
    }

    @Test(expected = MessagingException.class)
    public void clientCertifiedAndRevoked() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("testFlowClientRevoked");
        final MuleEvent res = flow.process(getTestEvent("data"));
    }
}
