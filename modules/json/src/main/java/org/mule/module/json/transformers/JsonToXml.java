/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.json.transformers;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.IOUtils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stax.StAXSource;
import java.io.File;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;

import de.odysseus.staxon.json.JsonXMLInputFactory;

/**
 * Convert JSON to an XML document string
 */
public class JsonToXml extends AbstractToFromXmlTransformer
{
    public JsonToXml()
    {
        this.registerSourceType(DataTypeFactory.STRING);
        this.registerSourceType(DataTypeFactory.INPUT_STREAM);
        this.registerSourceType(DataTypeFactory.BYTE_ARRAY);
        this.registerSourceType(DataTypeFactory.create(Reader.class));
        this.registerSourceType(DataTypeFactory.create(URL.class));
        this.registerSourceType(DataTypeFactory.create(File.class));
        setReturnDataType(DataTypeFactory.XML_STRING);
    }


    /**
     * Use Staxon to convert JSON to an XML string
     */
    @Override
    protected Object doTransform(Object src, String enc) throws TransformerException
    {
        XMLInputFactory inputFactory = new JsonXMLInputFactory();
        inputFactory.setProperty(JsonXMLInputFactory.PROP_MULTIPLE_PI, false);
        TransformerInputs inputs = new TransformerInputs(this,src);
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

            XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
            return convert(source, outputFactory);
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
