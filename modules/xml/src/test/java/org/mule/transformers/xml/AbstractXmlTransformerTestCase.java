/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml;

import org.mule.module.xml.util.XMLUtils;
import org.mule.transformer.AbstractTransformerTestCase;

import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.custommonkey.xmlunit.XMLUnit;
import org.w3c.dom.Document;

/**
 * Use this superclass if you intend to compare Xml contents.
 */
public abstract class AbstractXmlTransformerTestCase extends AbstractTransformerTestCase
{

    protected AbstractXmlTransformerTestCase()
    {
        super();
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setXSLTVersion("2.0");
        try
        {
            XMLUnit.getTransformerFactory();
        }
        catch (TransformerFactoryConfigurationError e)
        {
            XMLUnit.setTransformerFactory(XMLUtils.TRANSFORMER_FACTORY_JDK5);
        }
    }

    @Override
    public boolean compareResults(Object expected, Object result)
    {
        if (expected instanceof Document && result instanceof Document)
        {
            return XMLUnit.compareXML((Document)expected, (Document)result).similar();
        }
        else if (expected instanceof String && result instanceof String)
        {
            try
            {
                String expectedString = this.normalizeString((String)expected);
                String resultString = this.normalizeString((String)result);
                return XMLUnit.compareXML(expectedString, resultString).similar();
            }
            catch (Exception ex)
            {
                return false;
            }
        }
        else if (expected instanceof XMLStreamReader && result instanceof XMLStreamReader)
        {
            XMLStreamReader expectedStream = (XMLStreamReader) expected;
            XMLStreamReader resultStream = (XMLStreamReader) result;

            try
            {
                Document expectedDocument = XMLUtils.toW3cDocument(expectedStream);
                Document resultDocument = XMLUtils.toW3cDocument(resultStream);

                return XMLUnit.compareXML(expectedDocument, resultDocument).similar();
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        // all other comparisons are passed up
        return super.compareResults(expected, result);
    }

}
