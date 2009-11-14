/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.json.JsonData;
import org.mule.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


/**
 * A transformer that will convert a JSON encoded object graph to a java object. The object type is
 * determined by the 'returnClass' attribute. Note that this transformers supports Arrays and Lists. For
 * example, to
 * convert a JSON string to an array of org.foo.Person, set the the returnClass=[Lorg.foo.Person;.
 * <p/>
 * The JSON engine can be configured using the jsonConfig attribute. This is an object reference to an
 * instance of: {@link net.sf.json.JsonConfig}. This can be created as a spring bean.
 */
public class JsonToObject extends AbstractJsonTransformer
{
    private Map<Class, Class> deserializationMixins = new HashMap<Class, Class>();


    public JsonToObject()
    {
        this.registerSourceType(Reader.class);
        this.registerSourceType(URL.class);
        this.registerSourceType(File.class);
        this.registerSourceType(String.class);
        this.registerSourceType(InputStream.class);
        this.registerSourceType(byte[].class);
        setReturnClass(JsonData.class);
    }


    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        //Add shared mixins first
        for (Map.Entry<Class, Class> entry : getMixins().entrySet())
        {
            getMapper().getDeserializationConfig().addMixInAnnotations(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<Class, Class> entry : deserializationMixins.entrySet())
        {
            getMapper().getDeserializationConfig().addMixInAnnotations(entry.getKey(), entry.getValue());
        }


    }

    public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
    {
        Object src = message.getPayload();
        Object returnValue;
        InputStream is = null;

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
                if (getReturnClass().equals(JsonData.class))
                {
                    returnValue = new JsonData((Reader) src);
                }
                else
                {
                    returnValue = getMapper().readValue((Reader) src, getReturnClass());
                }
            }
            else if (src instanceof String)
            {
                if (getReturnClass().equals(JsonData.class))
                {
                    returnValue = new JsonData((String) src);
                }
                else
                {
                    returnValue = getMapper().readValue((String) src, getReturnClass());
                }
            }
            else
            {
                if (getReturnClass().equals(JsonData.class))
                {
                    returnValue = new JsonData((Reader) src);
                }
                else
                {
                    returnValue = getMapper().readValue(is, getReturnClass());
                }
            }
            return returnValue;
        }
        catch (Exception e)
        {
            throw new TransformerException(CoreMessages.transformFailed("json", getReturnClass().getName()), this, e);
        }
        finally
        {
            if (is != null)
            {
                IOUtils.closeQuietly(is);
            }
        }
    }

    public Map<Class, Class> getDeserializationMixins()
    {
        return deserializationMixins;
    }

    public void setDeserializationMixins(Map<Class, Class> deserializationMixins)
    {
        this.deserializationMixins = deserializationMixins;
    }
}
