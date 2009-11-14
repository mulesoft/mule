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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Converts a java object to a JSON encoded object that can be consumed by other languages such as
 * Javascript or Ruby.
 * <p/>
 * The JSON engine can be configured using the jsonConfig attribute. This is an object reference to an
 * instance of: {@link net.sf.json.JsonConfig}. This can be created as a spring bean.
 * <p/>
 * Users can configure a comma-separated list of property names to exclude or include i.e.
 * excludeProperties="address,postcode".
 * <p/>
 * The returnClass for this transformer is always java.lang.String, there is no need to set this.
 */
public class ObjectToJson extends AbstractJsonTransformer
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(ObjectToJson.class);

    private Map<Class, Class> serializationMixins = new HashMap<Class, Class>();

    protected Class sourceClass;

    private boolean handleException = false;

    public ObjectToJson()
    {
        this.registerSourceType(Object.class);
        this.setReturnClass(String.class);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();

        //restrict the handled types
        if (getSourceClass() != null)
        {
            sourceTypes.clear();
            registerSourceType(getSourceClass());
        }

        //Add shared mixins first
        for (Map.Entry<Class, Class> entry : getMixins().entrySet())
        {
            getMapper().getSerializationConfig().addMixInAnnotations(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<Class, Class> entry : serializationMixins.entrySet())
        {
            getMapper().getSerializationConfig().addMixInAnnotations(entry.getKey(), entry.getValue());
        }
        
    }

    public Object transform(MuleMessage message, String encoding) throws TransformerException
    {
        Object src = message.getPayload();


// Checks if there's an exception
        if (message.getExceptionPayload() != null && this.isHandleException())
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Found exception with null payload");
            }
            src = this.getException(message.getExceptionPayload().getException());
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try
        {
            getMapper().writeValue(baos, src);
        }
        catch (IOException e)
        {
            throw new TransformerException(this, e);
        }

        System.out.println(baos.toString());
        if(returnClass.equals(byte[].class))
        {
            return baos.toByteArray();
        }
        else
        {
            return baos.toString();
        }
    }

    /**
     * The reason of having this is because the original exception object is way too
     * complex and it breaks JSON-lib.
     */
    private Exception getException(Throwable t)
    {

        Exception returnValue = null;
        List causeStack = new ArrayList();

        for (Throwable tempCause = t; tempCause != null; tempCause = tempCause.getCause())
        {
            causeStack.add(tempCause);
        }

        for (int i = causeStack.size() - 1; i >= 0; i--)
        {
            Throwable tempCause = (Throwable) causeStack.get(i);

            // There is no cause at the very root
            if (i == causeStack.size())
            {
                returnValue = new Exception(tempCause.getMessage());
                returnValue.setStackTrace(tempCause.getStackTrace());
            }
            else
            {
                returnValue = new Exception(tempCause.getMessage(), returnValue);
                returnValue.setStackTrace(tempCause.getStackTrace());
            }
        }

        return returnValue;
    }

    // Getter/Setter
    // -------------------------------------------------------------------------
    public boolean isHandleException()
    {
        return this.handleException;
    }

    public void setHandleException(boolean handleException)
    {
        this.handleException = handleException;
    }

    public Class getSourceClass()
    {
        return sourceClass;
    }

    public void setSourceClass(Class sourceClass)
    {
        this.sourceClass = sourceClass;
    }

    public Map<Class, Class> getSerializationMixins()
    {
        return serializationMixins;
    }

    public void setSerializationMixins(Map<Class, Class> serializationMixins)
    {
        this.serializationMixins = serializationMixins;
    }
}

