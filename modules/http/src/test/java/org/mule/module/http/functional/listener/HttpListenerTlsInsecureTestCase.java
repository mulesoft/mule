/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import static org.junit.rules.ExpectedException.none;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Sets up two HTTPS servers with regular trust-stores, except one is insecure.
 * Verifies that a request using a certificate not present in the trust-store
 * only works for the insecure server.
 */
public class HttpListenerTlsInsecureTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");
    @Rule
    public DynamicPort port2 = new DynamicPort("port2");
    @Rule
    public ExpectedException expectedException = none();

    @Override
    protected String getConfigFile()
    {
        return "http-listener-insecure-config.xml";
    }

    @Test
    public void acceptsInvalidCertificateIfInsecure() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("testRequestToInsecure");
        final MuleEvent res = flow.process(getTestEvent(TEST_PAYLOAD));
        assertThat(res.getMessage().getPayloadAsString(), is(TEST_PAYLOAD));
    }

    @Test
    public void rejectsInvalidCertificateIfSecure() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("testRequestToSecure");
        expectedException.expect(MessagingException.class);
        expectedException.expectCause(isA(IOException.class));
        expectedException.expectCause(hasMessage(containsString("Remotely close")));
        flow.process(getTestEvent("data"));
    }
}
