/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import org.mule.transformer.AbstractTransformer;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stax.StAXResult;
import java.io.StringWriter;

/**
 * Superclass for transformers that convert JSON to and from XML
 */
public abstract class AbstractToFromXmlTransformer extends AbstractTransformer
{
    TransformerFactory transformerFactory;

    protected AbstractToFromXmlTransformer()
    {
        transformerFactory = TransformerInputs.createTransformerFactory();
    }

    /**
     * Return result of transformation
     */
    protected String convert(Source source, XMLOutputFactory factory) throws XMLStreamException, TransformerException
    {
        StringWriter writer = new StringWriter();
        XMLStreamWriter output = factory.createXMLStreamWriter(writer);
        Result result = new StAXResult(output);
        Transformer transformer = transformerFactory.newTransformer();
        transformer.transform(source, result);
        return writer.toString();
    }
}
