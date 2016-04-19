/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import org.mule.runtime.core.api.endpoint.EndpointBuilder;
import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.config.i18n.LocaleMessageHandler;
import org.mule.runtime.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.runtime.core.transformer.AbstractTransformerTestCase;
import org.mule.runtime.core.transformer.types.DataTypeFactory;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

import org.junit.Ignore;
import org.junit.Test;

public class StringToObjectArrayTestCase extends AbstractTransformerTestCase
{

    private String encoding = "Windows-31J";

    @Override
    public Object getResultData()
    {
        return new String[]{ getMessage("char0"), getMessage("char1"), getMessage("char2") };
    }

    @Override
    public Object getTestData()
    {
        try
        {
            return getMessage("message").getBytes(encoding);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Ignore
    @Test
    @Override
    public void testTransform() throws Exception
    {
        // Overriden just to ignore it
    }

    @Ignore
    @Test
    @Override
    public void testRoundtripTransform() throws Exception
    {
        // Overriden just to ignore it
    }
    
    @Override
    public Transformer getTransformer() throws Exception
    {
        Transformer trans = createObject(StringToObjectArray.class);
        trans.setReturnDataType(DataTypeFactory.create(Object[].class));

        EndpointBuilder builder = new EndpointURIEndpointBuilder("test://test", muleContext);
        builder.setEncoding(encoding);
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint(
                builder);
        trans.setEndpoint(endpoint);

        return trans;
    }

    @Override
    public Transformer getRoundTripTransformer() throws Exception
    {
        return new ObjectArrayToString();
    }

    @Override
    public boolean compareResults(Object expected, Object result)
    {
        return super.compareResults(expected, result);
    }

    @Override
    public boolean compareRoundtripResults(Object expected, Object result)
    {
        try
        {
            return super.compareRoundtripResults(expected, ((String)result).getBytes(encoding));
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
    }

    private String getMessage(String key)
    {
        return LocaleMessageHandler.getString("test-data", Locale.JAPAN, "StringToObjectArrayTestCase." + key, new Object[] {});
    }

}
