/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.transformers;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mule.compatibility.transport.http.HttpConstants.DEFAULT_CONTENT_TYPE;
import static org.mule.compatibility.transport.http.HttpConstants.FORM_URLENCODED_CONTENT_TYPE;
import static org.mule.compatibility.transport.http.HttpConstants.HEADER_CONTENT_TYPE;
import static org.mule.compatibility.transport.http.HttpConstants.METHOD_GET;
import static org.mule.compatibility.transport.http.HttpConstants.METHOD_POST;
import static org.mule.compatibility.transport.http.HttpConstants.METHOD_PUT;

import org.mule.compatibility.transport.http.transformers.HttpRequestBodyToParamMap;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.TransformationService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class HttpRequestBodyToParamMapTestCase extends AbstractMuleTestCase
{

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MuleContext muleContext;
    @Mock
    private TransformationService transformationService;

    @Before
    public void setup() throws Exception
    {
        when(muleContext.getTransformationService()).thenReturn(transformationService);
        when(transformationService.transform(any(MuleMessage.class), any(DataType.class))).thenAnswer(inv -> (MuleMessage) inv.getArguments()[0]);
    }

    @Test
    public void validGet() throws TransformerException
    {
        MuleMessage msg = createMessage(METHOD_GET, DEFAULT_CONTENT_TYPE);
        verifyTransformation(transform(new DefaultMuleEvent(msg, (FlowConstruct) null)));
    }

    @Test
    public void validPost() throws TransformerException
    {
        MuleMessage msg = createMessage(METHOD_POST, FORM_URLENCODED_CONTENT_TYPE);
        verifyTransformation(transform(new DefaultMuleEvent(msg, (FlowConstruct) null)));
    }

    @Test
    public void validPut() throws TransformerException
    {
        MuleMessage msg = createMessage(METHOD_PUT, FORM_URLENCODED_CONTENT_TYPE);
        verifyTransformation(transform(new DefaultMuleEvent(msg, (FlowConstruct) null)));
    }

    @Test(expected = TransformerException.class)
    public void invalidContentType() throws TransformerException
    {
        MuleMessage msg = createMessage(METHOD_POST, "application/json");
        transform(new DefaultMuleEvent(msg, (FlowConstruct) null));
    }

    private Object transform(MuleEvent event) throws TransformerException
    {
        HttpRequestBodyToParamMap transformer = new HttpRequestBodyToParamMap();
        transformer.setMuleContext(muleContext);
        return transformer.transformMessage(event, "UTF-8");
    }

    private void verifyTransformation(Object payload) throws TransformerException
    {
        assertThat(payload instanceof Map, is(true));
        Map<String, String> map = (Map<String, String>) payload;
        assertThat(map.size(), is(2));
        assertThat(map.get("key1"), is("value1"));
        assertThat(map.get("key2"), is("value2"));
    }

    private MuleMessage createMessage(String method, String contentType)
    {
        Map<String, Object> inboundProperties = new HashMap<String, Object>();
        inboundProperties.put("http.method", method);
        inboundProperties.put(HEADER_CONTENT_TYPE, contentType);

        String payload = "key1=value1&key2=value2";
        if ("GET".equals(method))
        {
            payload = "http://localhost/?" + payload;
        }
        return new DefaultMuleMessage(payload, inboundProperties, null, null, muleContext);
    }

}
