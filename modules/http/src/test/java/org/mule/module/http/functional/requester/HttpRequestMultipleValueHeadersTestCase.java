/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpHeaders.Values.CHUNKED;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.util.CaseInsensitiveMapWrapper;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

public class HttpRequestMultipleValueHeadersTestCase extends AbstractHttpRequestTestCase
{

    @Rule
    public SystemProperty host = new SystemProperty("host", "localhost");
    @Rule
    public SystemProperty encoding = new SystemProperty("encoding" , CHUNKED);
    @Rule
    public DynamicPort port = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "http-request-multiple-header-config.xml";
    }

    public HttpRequestMultipleValueHeadersTestCase()
    {
        headers = Multimaps.newListMultimap(new CaseInsensitiveMapWrapper<Collection<String>>(HashMap.class), new Supplier<List<String>>()
        {
            @Override
            public List<String> get()
            {
                return Lists.newArrayList();
            }
        });
    }

    @Test
    public void preservesOrderAndFormatWithMultipleValuedOutboundProperties() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE);

        event.getMessage().setOutboundProperty("multipleProperty", Arrays.asList("1", "2", "3"));
        processEventInFlow(event, "outboundProperties");

        assertThat(headers.asMap(), hasKey("multipleProperty"));
        assertThat(headers.asMap().get("multipleProperty"), isA(Iterable.class));
        assertThat(headers.asMap().get("multipleProperty"), contains("1", "2", "3"));
    }

    @Test
    public void receivesMultipleHeaders() throws Exception
    {
        MuleEvent event = runFlow("inboundProperties");

        Object header = event.getMessage().getInboundProperty("header");
        assertThat(header, instanceOf(Collection.class));
        Collection<String> headerValue = (Collection<String>) header;
        assertThat(headerValue, hasSize(2));
        assertThat(headerValue, containsInAnyOrder("customValue1", "customValue2"));
    }

    private void processEventInFlow(MuleEvent event, String flowName) throws Exception
    {
        Flow flow = (Flow) getFlowConstruct(flowName);
        flow.process(event);
    }
}


