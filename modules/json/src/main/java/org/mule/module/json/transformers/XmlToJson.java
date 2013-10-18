/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;


import org.mule.api.transformer.TransformerException;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.IOUtils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXSource;
import java.io.File;
import java.io.Reader;
import java.net.URL;

import de.odysseus.staxon.json.JsonXMLOutputFactory;
import org.w3c.dom.Document;

/**
 * Convert XML to a JSON string
 */
public class XmlToJson  extends AbstractToFromXmlTransformer
{
    public XmlToJson()
    {
        this.registerSourceType(DataTypeFactory.STRING);
        this.registerSourceType(DataTypeFactory.INPUT_STREAM);
        this.registerSourceType(DataTypeFactory.BYTE_ARRAY);
        this.registerSourceType(DataTypeFactory.create(Reader.class));
        this.registerSourceType(DataTypeFactory.create(URL.class));
        this.registerSourceType(DataTypeFactory.create(File.class));
        registerSourceType(DataTypeFactory.create(Document.class));
        this.setReturnDataType(DataTypeFactory.JSON_STRING);
    }

    /**
     * Use Staxon to convert XML to a JSON string
     */
    @Override
    protected Object doTransform(Object src, String enc) throws TransformerException
    {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        inputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);

        TransformerInputs inputs = null;
        Source source;
        try
        {
            if (src instanceof Document)
            {
                Document doc = (Document) src;
                String location = doc.getDocumentURI();
                if (location == null)
                {
                    location = "(Document)";
                }
                source = new DOMSource(doc, location);
            }
            else
            {
                inputs = new TransformerInputs(this, src);
                if (inputs.getInputStream() != null)
                {
                    source = new StAXSource(inputFactory.createXMLStreamReader(inputs.getInputStream(),  enc == null ? "UTF-8" : enc));
                }
                else
                {
                    source = new StAXSource(inputFactory.createXMLStreamReader(inputs.getReader()));
                }
            }
            XMLOutputFactory outputFactory = new JsonXMLOutputFactory();
            outputFactory.setProperty(JsonXMLOutputFactory.PROP_AUTO_ARRAY, true);
            outputFactory.setProperty(JsonXMLOutputFactory.PROP_PRETTY_PRINT, true);
           return convert(source, outputFactory);
        }
        catch (Exception ex)
        {
            throw new TransformerException(this, ex);
        }
        finally
        {
            if (inputs != null)
            {
                IOUtils.closeQuietly(inputs.getInputStream());
                IOUtils.closeQuietly(inputs.getReader());
            }
        }
    }
}
