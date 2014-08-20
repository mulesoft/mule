/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp.integration;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;

import java.net.SocketTimeoutException;

import org.junit.Rule;
import org.junit.Test;

public class HostResolutionTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "host-resolution-config.xml";
    }

    private void assertTimeoutException(String endpoint) throws MuleException
    {
        final MuleMessage message = muleContext.getClient().send(endpoint, "something", null);
        assertThat(message.getExceptionPayload(), is(not(nullValue())));
        assertThat(message.getExceptionPayload().getRootException(), is(instanceOf(SocketTimeoutException.class)));
        assertThat((NullPayload) message.getPayload(), is(sameInstance(NullPayload.getInstance())));
    }

    @Test
    public void testDefaultConfiguration() throws MuleException
    {
        assertTimeoutException("vm://defaultConfiguration");
    }

    @Test
    public void testFailOnUnresolvedHost_false() throws MuleException
    {
        assertTimeoutException("vm://failOnUnresolvedHostFalse");
    }

    @Test
    public void testFailOnUnresolvedHost_true() throws MuleException
    {
        assertTimeoutException("vm://failOnUnresolvedHostTrue");
    }

}
