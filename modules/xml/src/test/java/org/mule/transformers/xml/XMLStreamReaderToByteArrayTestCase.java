/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml;

import org.mule.api.transformer.Transformer;
import org.mule.module.xml.transformer.XMLStreamReaderToByteArray;
import org.mule.module.xml.transformer.XmlToXMLStreamReader;
import org.mule.module.xml.util.XMLUtils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

public class XMLStreamReaderToByteArrayTestCase extends AbstractXmlTransformerTestCase
{

    private static final String TEST_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><test>TEST_MESSAGE</test>";


    @Override
    public Transformer getTransformer() throws Exception
    {
        return createObject(XMLStreamReaderToByteArray.class);
    }

    @Override
    public Transformer getRoundTripTransformer() throws Exception
    {
        return createObject(XmlToXMLStreamReader.class);
    }

    @Override
    public Object getTestData()
    {
        try
        {
            return XMLUtils.toXMLStreamReader(XMLInputFactory.newFactory(), TEST_XML);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object getResultData()
    {
        return TEST_XML.getBytes();
    }
}
