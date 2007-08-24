/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.xml;

import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.IOUtils;

import org.dom4j.DocumentHelper;
import org.dom4j.io.DOMReader;
import org.dom4j.io.DOMWriter;
import org.w3c.dom.Document;

public class DomXmlTransformerEncodingTestCase extends AbstractXmlTransformerTestCase
{
    private Document srcData; // Parsed XML doc
    private String resultData; // String as US-ASCII

    // @Override
    protected void doSetUp() throws Exception
    {
        org.dom4j.Document dom4jDoc = DocumentHelper.parseText(IOUtils.toString(IOUtils.getResourceAsStream(
            "cdcatalog-utf-8.xml", getClass()), "UTF-8"));
        srcData = new DOMWriter().write(dom4jDoc);
        resultData = IOUtils.toString(IOUtils.getResourceAsStream("cdcatalog-us-ascii.xml", getClass()),
            "US-ASCII");
    }

    public UMOTransformer getTransformer() throws Exception
    {
        UMOTransformer trans = new DomDocumentToXml();
        trans.setReturnClass(String.class);

        UMOEndpoint endpoint = new MuleEndpoint();
        endpoint.setEncoding("US-ASCII");
        trans.setEndpoint(endpoint);
        return trans;
    }

    public UMOTransformer getRoundTripTransformer() throws Exception
    {
        return new XmlToDomDocument(); // encoding is not interesting
    }

    public Object getTestData()
    {
        return srcData;
    }

    public Object getResultData()
    {
        return resultData;
    }

    // @Override
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
