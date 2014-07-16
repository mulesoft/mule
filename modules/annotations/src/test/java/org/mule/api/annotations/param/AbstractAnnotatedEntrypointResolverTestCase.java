/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.param;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleEventContext;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEventContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.model.EntryPointResolver;
import org.mule.api.model.InvocationResult;
import org.mule.impl.model.resolvers.AnnotatedEntryPointResolver;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.IOUtils;
import org.mule.util.StringDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;

public abstract class AbstractAnnotatedEntrypointResolverTestCase extends AbstractMuleContextTestCase
{
    protected MuleEventContext eventContext;
    protected boolean inboundScope = true;

    @Override
    public void doSetUp() throws Exception
    {
        super.doSetUp();

        try
        {
            eventContext = createEventContext(null, null);
        }
        catch (Exception e)
        {
            fail(e.getMessage());
        }
    }

    protected MuleEventContext createEventContext(Map<String, Object> headers, Map<String, DataHandler> attachments) throws Exception
    {
        if(headers==null)
        {
            headers = new HashMap<String, Object>();
            headers.put("foo", "fooValue");
            headers.put("bar", "barValue");
            headers.put("baz", "bazValue");
        }

        if(attachments==null)
        {
            attachments = new HashMap<String, DataHandler>();
            attachments.put("foo", new DataHandler(new StringDataSource("fooValue")));
            attachments.put("bar", new DataHandler(new StringDataSource("barValue")));
            attachments.put("baz", new DataHandler(new StringDataSource("bazValue")));
        }
        DefaultMuleMessage message;
        if(inboundScope)
        {
            message = new DefaultMuleMessage("test", null, attachments, muleContext);
            for (String s : headers.keySet())
            {
                message.setInboundProperty(s, headers.get(s));
            }
        }
        else
        {
            message = new DefaultMuleMessage("test", headers, muleContext);
            for (String s : attachments.keySet())
            {
                message.addOutboundAttachment(s, attachments.get(s));
            }
        }
        return new DefaultMuleEventContext(new DefaultMuleEvent(message, getTestInboundEndpoint("null"),
            (FlowConstruct) null));
    }

    protected InvocationResult invokeResolver(String methodName, MuleEventContext eventContext) throws Exception
    {
        EntryPointResolver resolver = getResolver();
        eventContext.getMessage().setInvocationProperty(MuleProperties.MULE_METHOD_PROPERTY, methodName);
        InvocationResult result = resolver.invoke(getComponent(), eventContext);
        if (InvocationResult.State.SUCCESSFUL == result.getState())
        {
            assertNotNull("The result of invoking the component should not be null", result.getResult());
            assertNull(result.getErrorMessage());
            assertFalse(result.hasError());
            assertEquals(methodName, result.getMethodCalled());
        }
        return result;
    }

    protected EntryPointResolver getResolver() throws Exception
    {
        return createObject(AnnotatedEntryPointResolver.class);
    }

    protected abstract Object getComponent();

    protected String readAttachment(DataHandler handler) throws IOException
    {
        return IOUtils.toString((InputStream) handler.getContent());
    }
}
