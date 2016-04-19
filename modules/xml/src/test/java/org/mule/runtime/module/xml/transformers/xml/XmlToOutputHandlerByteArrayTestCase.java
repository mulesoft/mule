/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.transformers.xml;

import static org.junit.Assert.fail;

import org.mule.runtime.core.api.endpoint.EndpointBuilder;
import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.core.transformer.types.DataTypeFactory;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.xml.transformer.XmlToOutputHandler;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.dom4j.DocumentHelper;
import org.dom4j.io.DOMWriter;

public class XmlToOutputHandlerByteArrayTestCase extends AbstractXmlTransformerTestCase
{
    private byte[] srcData;
    private String resultData;

    @Override
    protected void doSetUp() throws Exception
    {
        InputStream resourceStream = IOUtils.getResourceAsStream("cdcatalog-utf-8.xml", getClass());
        resultData = IOUtils.toString(resourceStream, "UTF-8");
        
        srcData = resultData.getBytes("UTF-8");
    }

    @Override
    public Transformer getTransformer() throws Exception
    {
        Transformer trans = createObject(XmlToOutputHandler.class);
        trans.setReturnDataType(DataTypeFactory.create(OutputHandler.class));

        EndpointBuilder builder = new EndpointURIEndpointBuilder("test://test", muleContext);
        builder.setEncoding("UTF-8");
        ImmutableEndpoint endpoint =
            muleContext.getEndpointFactory().getInboundEndpoint(builder);

        trans.setEndpoint(endpoint);
        return trans;
    }

    @Override
    public Transformer getRoundTripTransformer() throws Exception
    {
        return null;
    }

    @Override
    public Object getTestData()
    {
        return srcData;
    }

    @Override
    public Object getResultData()
    {
        return resultData;
    }

    @Override
    public boolean compareResults(Object expected, Object result)
    {
        if (result instanceof OutputHandler)
        {
            OutputHandler handler = (OutputHandler)result;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try
            {
                handler.write(null, bos);
                org.dom4j.Document dom4jDoc = null;
                dom4jDoc = DocumentHelper.parseText((String)expected);
                expected = new DOMWriter().write(dom4jDoc);
                dom4jDoc = DocumentHelper.parseText(new String(bos.toByteArray(), "UTF-8"));
                result = new DOMWriter().write(dom4jDoc);
            }
            catch (Exception e)
            {
                fail();
            }
        }
        return super.compareResults(expected, result);
    }
}
