/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

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
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Sets up two HTTPS clients using a regular trust-store, but one of them insecure.
 * Then two HTTPS servers: one will return a certificate present in the trust-store
 * but with an invalid SAN extension, the other will return a certificate that's not in the trust-store.
 * Verifies that only the insecure client is successful.
 */
@RunWith(Parameterized.class)
public class HttpRequestTlsInsecureTestCase extends FunctionalTestCase
{
    @Parameterized.Parameter
    public String config;

    @Rule
    public DynamicPort port1 = new DynamicPort("port1");
    @Rule
    public SystemProperty insecure = new SystemProperty("insecure", "true");
    @Rule
    public ExpectedException expectedException = none();

    @Override
    protected String getConfigFile()
    {
        return config;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"http-request-insecure-hostname-config.xml"},
                {"http-request-insecure-certificate-config.xml"}});
    }

    @Test
    public void insecureRequest() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("testInsecureRequest");
        final MuleEvent res = flow.process(getTestEvent(TEST_PAYLOAD));
        assertThat(res.getMessage().getPayloadAsString(), is(TEST_PAYLOAD));
    }

    @Test
    public void secureRequest() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("testSecureRequest");
        expectedException.expect(MessagingException.class);
        expectedException.expectCause(isA(IOException.class));
        expectedException.expectCause(hasMessage(containsString("General SSLEngine problem")));
        flow.process(getTestEvent(TEST_PAYLOAD));
    }

}