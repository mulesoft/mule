/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.simple;

import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transformer.Transformer;
import org.mule.config.i18n.LocaleMessageHandler;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.transformer.AbstractTransformerTestCase;
import org.mule.transformer.types.DataTypeFactory;

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
