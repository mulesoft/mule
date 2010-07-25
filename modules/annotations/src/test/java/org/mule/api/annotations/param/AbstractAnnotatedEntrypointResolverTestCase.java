/*
 * $Id:  $
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.param;

import org.mule.api.MuleEventContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.model.EntryPointResolver;
import org.mule.api.model.InvocationResult;
import org.mule.impl.model.resolvers.AnnotatedEntryPointResolver;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.IOUtils;
import org.mule.util.StringDataSource;

import java.io.IOException;
import java.io.InputStream;

import javax.activation.DataHandler;

public abstract class AbstractAnnotatedEntrypointResolverTestCase extends AbstractMuleTestCase
{
    protected MuleEventContext eventContext;

    @Override
    public void doSetUp() throws Exception
    {
        super.doSetUp();

        eventContext = getTestEventContext("test");
        eventContext.getMessage().setInboundProperty("foo", "fooValue");
        eventContext.getMessage().setInboundProperty("bar", "barValue");
        eventContext.getMessage().setInboundProperty("baz", "bazValue");

        try
        {
            eventContext.getMessage().addAttachment("foo", new DataHandler(new StringDataSource("fooValue")));
            eventContext.getMessage().addAttachment("bar", new DataHandler(new StringDataSource("barValue")));
            eventContext.getMessage().addAttachment("baz", new DataHandler(new StringDataSource("bazValue")));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
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
