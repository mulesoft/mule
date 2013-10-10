/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.transformer;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.store.DeserializationPostInitialisable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

/**
 * <code>XmlToObject</code> converts xml created by the ObjectToXml transformer in to a
 * java object graph. This transformer uses XStream. Xstream uses some clever tricks so
 * objects that get marshalled to XML do not need to implement any interfaces including
 * Serializable and you don't even need to specify a default constructor.
 *
 * @see ObjectToXml
 */

public class XmlToObject extends AbstractXStreamTransformer
{

    private final DomDocumentToXml domTransformer = new DomDocumentToXml();

    public XmlToObject()
    {
        registerSourceType(DataTypeFactory.STRING);
        registerSourceType(DataTypeFactory.BYTE_ARRAY);
        registerSourceType(DataTypeFactory.INPUT_STREAM);
        registerSourceType(DataTypeFactory.create(org.w3c.dom.Document.class));
        registerSourceType(DataTypeFactory.create(org.dom4j.Document.class));
        setReturnDataType(DataTypeFactory.OBJECT);
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        Object src = message.getPayload();
        Object result;
        if (src instanceof byte[])
        {
            try
            {
                Reader xml = new InputStreamReader(new ByteArrayInputStream((byte[]) src), outputEncoding);
                result = getXStream().fromXML(xml);
            }
            catch (UnsupportedEncodingException e)
            {
                throw new TransformerException(this, e);
            }
        }
        else if (src instanceof InputStream)
        {
            InputStream input = (InputStream) src;
            try
            {
                Reader xml = new InputStreamReader(input, outputEncoding);
                result = getXStream().fromXML(xml);
            }
            catch (Exception e)
            {
                throw new TransformerException(this, e);
            }
            finally
            {
                try
                {
                    input.close();
                }
                catch (IOException e)
                {
                    logger.warn("Exception closing stream: ", e);
                }
            }
        }
        else if (src instanceof String)
        {
            result = getXStream().fromXML(src.toString());
        }
        else
        {
            result = getXStream().fromXML((String) domTransformer.transform(src));
        }

        try
        {
            postDeserialisationInit(result);
            return result;
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

    protected void postDeserialisationInit(final Object object) throws Exception
    {
        if (object instanceof DeserializationPostInitialisable)
        {
            DeserializationPostInitialisable.Implementation.init(object, muleContext);
        }
    }

}
