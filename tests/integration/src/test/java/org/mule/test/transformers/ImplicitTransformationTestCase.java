/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.transformers;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.io.InputStream;

import org.apache.tools.ant.filters.StringInputStream;
import org.junit.Test;

public class ImplicitTransformationTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/transformers/implicit-transformation-config.xml";
    }

    @Test
    public void testImplicitInputStreamToStringConversion() throws Exception
    {
        InputStream inputStream = new StringInputStream("TEST");
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://stringInput", inputStream, null);
        assertEquals("TSET", response.getPayload());
    }

    @Test
    public void testImplicitByteArrayToStringConversion() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://stringInput",  "TEST".getBytes(), null);
        assertEquals("TSET", response.getPayload());
    }

    @Test
    public void testImplicitInputStreamToByteArrayConversion() throws Exception
    {
        InputStream inputStream = new StringInputStream("TEST");
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://byteArrayInput", inputStream, null);
        assertEquals("TSET", response.getPayload());
    }

    @Test
    public void testImplicitStringToByteArrayConversion() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://byteArrayInput",  "TEST", null);
        assertEquals("TSET", response.getPayload());
    }

    @Test
    public void testImplicitStringToInputStreamConversion() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://inputStreamInput", "TEST", null);
        assertEquals("TSET", response.getPayload());
    }

    @Test
    public void testImplicitByteArrayToInputStreamConversion() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://inputStreamInput",  "TEST".getBytes(), null);
        assertEquals("TSET", response.getPayload());
    }

    public static class TestStringTransformer extends AbstractTransformer
    {

        public TestStringTransformer()
        {
            super();
            registerSourceType(DataTypeFactory.STRING);
            setReturnDataType(DataTypeFactory.STRING);
        }

        @Override
        protected Object doTransform(Object src, String enc) throws TransformerException
        {
            return StringUtils.reverse((String) src);
        }
    }

    public static class TestByteArrayTransformer extends AbstractTransformer
    {

        public TestByteArrayTransformer()
        {
            super();
            registerSourceType(DataTypeFactory.BYTE_ARRAY);
            setReturnDataType(DataTypeFactory.STRING);
        }

        @Override
        protected Object doTransform(Object src, String enc) throws TransformerException
        {
            return StringUtils.reverse(new String((byte[]) src));
        }
    }

    public static class TestInputStreamTransformer extends AbstractTransformer
    {

        public TestInputStreamTransformer()
        {
            super();
            registerSourceType(DataTypeFactory.INPUT_STREAM);
            setReturnDataType(DataTypeFactory.STRING);
        }

        @Override
        protected Object doTransform(Object src, String enc) throws TransformerException
        {

            InputStream input = (InputStream) src;
            String stringValue;
            try
            {
                stringValue = IOUtils.toString(input);
            }
            finally
            {
                IOUtils.closeQuietly(input);
            }
            return StringUtils.reverse(stringValue);
        }
    }
}
