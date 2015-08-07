/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.json.JsonData;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.MimeTypes;
import org.mule.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * A transformer that will convert a JSON encoded object graph to a java object. The
 * object type is determined by the 'returnType' attribute. Note that this
 * transformers supports Arrays and Lists. For example, to convert a JSON string to
 * an array of org.foo.Person, set the the returnClass=[Lorg.foo.Person;.
 */
public class JsonToObject extends AbstractJsonTransformer
{
    private static final DataType<JsonData> JSON_TYPE = DataTypeFactory.create(JsonData.class, MimeTypes.APPLICATION_JSON);

    private Map<Class<?>, Class<?>> deserializationMixins = new HashMap<Class<?>, Class<?>>();

    public JsonToObject()
    {
        this.registerSourceType(DataTypeFactory.create(Reader.class));
        this.registerSourceType(DataTypeFactory.create(URL.class));
        this.registerSourceType(DataTypeFactory.create(File.class));
        this.registerSourceType(DataTypeFactory.STRING);
        this.registerSourceType(DataTypeFactory.INPUT_STREAM);
        this.registerSourceType(DataTypeFactory.BYTE_ARRAY);
        setReturnDataType(JSON_TYPE);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        //Add shared mixins first
        for (Map.Entry<Class<?>, Class<?>> entry : getMixins().entrySet())
        {
            getMapper().getDeserializationConfig().addMixInAnnotations(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<Class<?>, Class<?>> entry : deserializationMixins.entrySet())
        {
            getMapper().getDeserializationConfig().addMixInAnnotations(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        Object src = message.getPayload();
        Object returnValue;
        InputStream is = null;
        Reader reader = null;

        try
        {
            if (src instanceof InputStream)
            {
                is = (InputStream) src;
            }
            else if (src instanceof File)
            {
                is = new FileInputStream((File) src);
            }
            else if (src instanceof URL)
            {
                is = ((URL) src).openStream();
            }
            else if (src instanceof byte[])
            {
                is = new ByteArrayInputStream((byte[]) src);
            }

            if (src instanceof Reader)
            {
                if (getReturnDataType().equals(JSON_TYPE))
                {
                    returnValue = new JsonData((Reader) src);
                }
                else
                {
                    returnValue = getMapper().readValue((Reader) src, getReturnDataType().getType());
                }
            }
            else if (src instanceof String)
            {
                if (getReturnDataType().equals(JSON_TYPE))
                {
                    returnValue = new JsonData((String) src);
                }
                else
                {
                    returnValue = getMapper().readValue((String) src, getReturnDataType().getType());
                }
            }
            else
            {
                reader = new InputStreamReader(is, outputEncoding);
                if (getReturnDataType().equals(JSON_TYPE))
                {
                    returnValue = new JsonData(reader);
                }
                else
                {
                    returnValue = getMapper().readValue(reader, getReturnDataType().getType());
                }
            }
            return returnValue;
        }
        catch (Exception e)
        {
            throw new TransformerException(CoreMessages.transformFailed("json",
                getReturnDataType().getType().getName()), this, e);
        }
        finally
        {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(is);
        }
    }

    public Map<Class<?>, Class<?>> getDeserializationMixins()
    {
        return deserializationMixins;
    }

    public void setDeserializationMixins(Map<Class<?>, Class<?>> deserializationMixins)
    {
        this.deserializationMixins = deserializationMixins;
    }
}
