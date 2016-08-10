/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transformers;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.StringUtils;

import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.tools.ant.filters.StringInputStream;
import org.junit.Test;

public class ImplicitTransformationTestCase extends AbstractIntegrationTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/transformers/implicit-transformation-config.xml";
    }

    @Test
    public void testImplicitInputStreamToStringConversion() throws Exception
    {
        InputStream inputStream = new StringInputStream("TEST");
        MuleMessage response = flowRunner("StringEchoService").withPayload(inputStream).run().getMessage();
        assertThat(response.getPayload(), is("TSET"));
    }

    @Test
    public void testImplicitByteArrayToStringConversion() throws Exception
    {
        MuleMessage response = flowRunner("StringEchoService").withPayload("TEST".getBytes()).run().getMessage();
        assertThat(response.getPayload(), is("TSET"));
    }

    @Test
    public void testImplicitInputStreamToByteArrayConversion() throws Exception
    {
        InputStream inputStream = new StringInputStream("TEST");
        MuleMessage response = flowRunner("ByteArrayEchoService").withPayload(inputStream).run().getMessage();
        assertThat(response.getPayload(), is("TSET"));
    }

    @Test
    public void testImplicitStringToByteArrayConversion() throws Exception
    {
        MuleMessage response = flowRunner("ByteArrayEchoService").withPayload("TEST").run().getMessage();
        assertThat(response.getPayload(), is("TSET"));
    }

    @Test
    public void testImplicitStringToInputStreamConversion() throws Exception
    {
        MuleMessage response = flowRunner("InputStreamEchoService").withPayload("TEST").run().getMessage();
        assertThat(response.getPayload(), is("TSET"));
    }

    @Test
    public void testImplicitByteArrayToInputStreamConversion() throws Exception
    {
        MuleMessage response = flowRunner("InputStreamEchoService").withPayload("TEST".getBytes()).run().getMessage();
        assertThat(response.getPayload(), is("TSET"));
    }

    public static class TestStringTransformer extends AbstractTransformer
    {

        public TestStringTransformer()
        {
            super();
            registerSourceType(DataType.STRING);
            setReturnDataType(DataType.STRING);
        }

        @Override
        protected Object doTransform(Object src, Charset enc) throws TransformerException
        {
            return StringUtils.reverse((String) src);
        }
    }

    public static class TestByteArrayTransformer extends AbstractTransformer
    {

        public TestByteArrayTransformer()
        {
            super();
            registerSourceType(DataType.BYTE_ARRAY);
            setReturnDataType(DataType.STRING);
        }

        @Override
        protected Object doTransform(Object src, Charset enc) throws TransformerException
        {
            return StringUtils.reverse(new String((byte[]) src));
        }
    }

    public static class TestInputStreamTransformer extends AbstractTransformer
    {

        public TestInputStreamTransformer()
        {
            super();
            registerSourceType(DataType.INPUT_STREAM);
            setReturnDataType(DataType.STRING);
        }

        @Override
        protected Object doTransform(Object src, Charset enc) throws TransformerException
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
