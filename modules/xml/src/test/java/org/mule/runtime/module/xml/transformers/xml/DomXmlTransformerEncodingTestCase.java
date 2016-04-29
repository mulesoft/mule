/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.transformers.xml;

import org.mule.runtime.core.api.endpoint.EndpointBuilder;
import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.runtime.core.transformer.types.DataTypeFactory;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.xml.transformer.DomDocumentToXml;
import org.mule.runtime.module.xml.transformer.XmlToDomDocument;

import org.dom4j.DocumentHelper;
import org.dom4j.io.DOMReader;
import org.dom4j.io.DOMWriter;
import org.w3c.dom.Document;

public class DomXmlTransformerEncodingTestCase extends AbstractXmlTransformerTestCase
{
    private Document srcData; // Parsed XML doc
    private String resultData; // String as US-ASCII

    @Override
    protected void doSetUp() throws Exception
    {
        org.dom4j.Document dom4jDoc = DocumentHelper.parseText(IOUtils.toString(IOUtils.getResourceAsStream(
            "cdcatalog-utf-8.xml", getClass()), "UTF-8"));
        srcData = new DOMWriter().write(dom4jDoc);
        resultData = IOUtils.toString(IOUtils.getResourceAsStream("cdcatalog-us-ascii.xml", getClass()),
            "US-ASCII");
    }

    @Override
    public Transformer getTransformer() throws Exception
    {
        Transformer trans = createObject(DomDocumentToXml.class);
        trans.setReturnDataType(DataTypeFactory.STRING);

        EndpointBuilder builder = new EndpointURIEndpointBuilder("test://test", muleContext);
        builder.setEncoding("US-ASCII");
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint(
            builder);

        trans.setEndpoint(endpoint);
        return trans;
    }

    @Override
    public Transformer getRoundTripTransformer() throws Exception
    {
        XmlToDomDocument trans = createObject(XmlToDomDocument.class); // encoding is not interesting
        trans.setReturnDataType(DataTypeFactory.create(org.w3c.dom.Document.class));
        return trans;
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
        // This is only used during roundtrip test, so it will always be Document
        // instances
        if (expected instanceof Document)
        {
            expected = new DOMReader().read((Document)expected).asXML();
            result = new DOMReader().read((Document)result).asXML();
        }

        return super.compareResults(expected, result);
    }

}
