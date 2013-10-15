/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.annotations;

import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.MuleProperties;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.module.ibeans.config.IBeanHolderConfigurationBuilder;
import org.mule.module.ibeans.spi.MuleIBeansPlugin;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ibeans.annotation.Return;
import org.ibeans.api.DataType;
import org.ibeans.api.Response;
import org.ibeans.api.channel.MimeType;
import org.ibeans.api.channel.MimeTypes;
import org.ibeans.impl.support.datatype.CollectionDataType;
import org.ibeans.impl.support.datatype.DataTypeFactory;
import org.ibeans.impl.test.MockIBean;
import org.ibeans.impl.test.MockMessageCallback;
import org.ibeans.spi.IBeansPlugin;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertNotNull;

public abstract class AbstractIBeansTestCase extends AbstractMuleContextTestCase
{
    protected IBeansPlugin plugin;

    protected AbstractIBeansTestCase()
    {
        setStartContext(true);
        setDisposeContextPerClass(true);
    }

    protected IBeansPlugin createPlugin()
    {
        return new MuleIBeansPlugin(muleContext);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        plugin = createPlugin();
        //register the test so that the @IntegrationBean annotation is processed
        muleContext.getRegistry().registerObject("testcase", this);
    }

    @Override
    protected void addBuilders(List<ConfigurationBuilder> builders)
    {
        IBeanHolderConfigurationBuilder builder = new IBeanHolderConfigurationBuilder("org.mule");
        builders.add(builder);
    }

    protected Answer withXmlData(final String resource, final Object ibean)
    {
        return withData(resource, MimeTypes.XML, null, ibean);
    }

    protected Answer withRssData(final String resource, final Object ibean)
    {
        return withData(resource, MimeTypes.RSS, null, ibean);
    }

    protected Answer withAtomData(final String resource, final Object ibean)
    {
        return withData(resource, MimeTypes.ATOM, null, ibean);
    }

    protected Answer withJsonData(final String resource, final Object ibean)
    {
        return withData(resource, MimeTypes.JSON, null, ibean);
    }

    protected Answer withTextData(final String resource, final Object ibean)
    {
        return withData(resource, MimeTypes.TEXT, null, ibean);
    }

    /**
     * A mock return for a method call that will load data and transform it into the return type set on the iBean.
     *
     * @param resource   the resource file name that contains the data you wish to load
     * @param returnType the Java type that the data should be converted to
     * @return a Mockito {@link org.mockito.stubbing.Answer} implementation that will load the data when requested
     */
    protected Answer withData(final String resource, final Class returnType)
    {
        return new Answer()
        {
            public Object answer(InvocationOnMock
                    invocation) throws Throwable
            {
                return loadData(resource, DataTypeFactory.create(returnType));
            }
        };
    }

    /**
     * A mock return for a method call that will load data and transform it into the return type set on the iBean.
     *
     * @param resource the resource file name that contains the data you wish to load
     * @param ibean    the ibean that is being tested
     * @param mimeType the mime type of the data
     * @param callback a callback can be used to manipulate the MuleMessage before it it gets returned
     * @return a Mockito {@link org.mockito.stubbing.Answer} implementation that will load the data when requested
     */
    protected Answer withData(final String resource, final MimeType mimeType, final MockMessageCallback callback, final Object ibean)
    {
        return new Answer()
        {
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                MimeType mime = mimeType;
                DataType ret = ((MockIBean)ibean).ibeanReturnType();
                if(ret!=null) ret.setMimeType(mime.toString());
                Object data;

                /**
                 * We need to have some special handling when dealing with a Mockito mock
                 * 1) If the return type on the ibeans is not set, use the method return type
                 * 2) the return annotation changes the return type so use the one defined on the actual Method
                 * 3) If the return type and the method return type are not assignable, then use the method return type
                 */
                if (ret == null || invocation.getMethod().isAnnotationPresent(Return.class) ||
                        !invocation.getMethod().getReturnType().isAssignableFrom(ret.getType()))
                {
                    ret = DataTypeFactory.createFromReturnType(invocation.getMethod());
                    mime = null;
                }

                data = loadData(resource, ret);
                ((MockIBean)ibean).ibeanSetMimeType(mime);
                ((MockIBean)ibean).ibeanSetMessageCallback(callback);

                Response response;
                Map<String, Object> headers = null;
                if(mime!=null)
                {
                    headers = new HashMap<String, Object>();
                    headers.put(MuleProperties.CONTENT_TYPE_PROPERTY, mime.toString());
                }
                response = plugin.createResponse(data, headers, null);
                if(callback!=null)
                {
                    callback.onMessage(response);
                }
                return response;
            }
        };
    }

    protected <T> T loadData(String resource, DataType<T> type) throws IOException, TransformerException
    {
        InputStream in = IOUtils.getResourceAsStream(resource, getClass());
        assertNotNull("Resource stream for: " + resource + " must not be null", in);
        return getDataAs(in, type);
    }

    protected <T> T getDataAs(InputStream data, DataType<T> as) throws TransformerException
    {
        org.mule.api.transformer.DataType muleDT;
        if(as instanceof CollectionDataType)
        {
            muleDT = new org.mule.transformer.types.CollectionDataType(
                    (Class<? extends Collection>)as.getType(),
                    ((CollectionDataType) as).getItemType(),
                    as.getMimeType());
        }
        else
        {
            muleDT = new org.mule.transformer.types.SimpleDataType(
                    as.getType(),
                    as.getMimeType());
        }
        Transformer t = muleContext.getRegistry().lookupTransformer(org.mule.transformer.types.DataTypeFactory.create(data.getClass()), muleDT);
        return (T)t.transform(data);
    }

}
