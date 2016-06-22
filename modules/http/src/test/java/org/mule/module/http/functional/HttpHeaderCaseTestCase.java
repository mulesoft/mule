/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.module.http.api.HttpHeaders.Names.USER_AGENT;
import static org.mule.module.http.api.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.http.internal.ParameterMap;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.HashSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;

/**
 * Set up a listener that returns form data and a requester that triggers it, all while preserving the headers name case.
 * That way we make sure the Host, Content-Type and other headers are handled correctly by both listener and requester.
 */
public class HttpHeaderCaseTestCase extends FunctionalTestCase
{
    public static final String PRESERVE_HEADER_CASE = "org.glassfish.grizzly.http.PRESERVE_HEADER_CASE";
    public static final String HEADER_NAME = "CusTomName";

    @Rule
    public DynamicPort port = new DynamicPort("port");
    @Rule
    public SystemProperty headerCaseProperty = new SystemProperty(PRESERVE_HEADER_CASE, "true");

    @Override
    protected String getConfigFile()
    {
        return "http-header-case-config.xml";
    }

    @Test
    public void worksPreservingHeaders() throws Exception
    {
        MuleEvent response = runFlow("client");
        Object payload = response.getMessage().getPayload();
        assertThat(payload, is(instanceOf(ParameterMap.class)));
        assertThat((ParameterMap) payload, hasEntry("key", "value"));
        assertThat((String) response.getMessage().getInboundProperty(CONTENT_TYPE), is(APPLICATION_X_WWW_FORM_URLENCODED));
        assertThat((String) response.getMessage().getInboundProperty(USER_AGENT), is("Mule 3.9.0"));
        assertThat((String) response.getMessage().getInboundProperty("customname1"), is("customValue"));
        assertThat((String) response.getMessage().getInboundProperty(HEADER_NAME), is("CustomValue"));
    }

    public static class InboundPropertyCaseMessageProcessor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            MuleMessage message = event.getMessage();
            Set<String> inboundPropertyNames = new HashSet<>(message.getInboundPropertyNames());
            message.setPayload(String.valueOf(inboundPropertyNames.contains(HEADER_NAME)));
            return event;
        }
    }
}
