/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.module.xml.transformer.XsltTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.IOUtils;

import de.odysseus.staxon.json.JsonXMLInputFactory;
import de.odysseus.staxon.json.JsonXMLOutputFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;
import java.io.File;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;

/**
 * Convert Json to Json using XSLT
 */
public class JsonXsltTransformer extends XsltTransformer
{
    public JsonXsltTransformer()
    {
        this.registerSourceType(DataTypeFactory.STRING);
        this.registerSourceType(DataTypeFactory.INPUT_STREAM);
        this.registerSourceType(DataTypeFactory.BYTE_ARRAY);
        this.registerSourceType(DataTypeFactory.create(Reader.class));
        this.registerSourceType(DataTypeFactory.create(URL.class));
        this.registerSourceType(DataTypeFactory.create(File.class));
        setReturnDataType(DataTypeFactory.XML_STRING);

        setXslTransformerFactory(TransformerInputs.getPreferredTransactionFactoryClassname());
    }

    /**
     * run a JSON to JSON XSLT transformationn XML string
     */
    @Override
    public Object transformMessage(MuleMessage message, String enc) throws TransformerException
    {
        XMLInputFactory inputFactory = new JsonXMLInputFactory();
        inputFactory.setProperty(JsonXMLInputFactory.PROP_MULTIPLE_PI, false);
        TransformerInputs inputs = new TransformerInputs(this, message.getPayload());
        Source source;
        try
        {
            if (inputs.getInputStream() != null)
            {
                source = new StAXSource(inputFactory.createXMLStreamReader(inputs.getInputStream(), enc == null ? "UTF-8" : enc));
            }
            else
            {
                source = new StAXSource(inputFactory.createXMLStreamReader(inputs.getReader()));
            }

            XMLOutputFactory outputFactory = new JsonXMLOutputFactory();
            outputFactory.setProperty(JsonXMLOutputFactory.PROP_AUTO_ARRAY, true);
            outputFactory.setProperty(JsonXMLOutputFactory.PROP_PRETTY_PRINT, true);
            StringWriter writer = new StringWriter();
            XMLStreamWriter output = outputFactory.createXMLStreamWriter(writer);
            Result result = new StAXResult(output);

            doTransform(message, enc, source, result);
            return writer.toString();
        }
        catch (Exception ex)
        {
            throw new TransformerException(this, ex);
        }
        finally
        {
            IOUtils.closeQuietly(inputs.getInputStream());
            IOUtils.closeQuietly(inputs.getReader());
        }
    }

}
