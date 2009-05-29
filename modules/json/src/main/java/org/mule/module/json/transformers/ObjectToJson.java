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
import org.mule.module.json.util.JsonUtils;
import org.mule.transformer.AbstractMessageAwareTransformer;
import org.mule.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import net.sf.json.JsonConfig;
import net.sf.json.util.PropertyFilter;

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
public class ObjectToJson extends AbstractMessageAwareTransformer
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(ObjectToJson.class);

    protected JsonConfig jsonConfig;

    protected String excludeProperties;
    protected String includeProperties;

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
        if (getReturnClass().equals(Object.class))
        {
            logger.warn("The return class is not set not type validation will be done");
        }

        if (excludeProperties != null)
        {
            getJsonConfig().setExcludes(StringUtils.splitAndTrim(excludeProperties, ","));
        }

        if (includeProperties != null)
        {
            getJsonConfig().setJsonPropertyFilter(new IncludePropertiesFilter(StringUtils.splitAndTrim(includeProperties, ",")));
        }

        //restrict the handled types
        if (getSourceClass() != null)
        {
            sourceTypes.clear();
            registerSourceType(getSourceClass());
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

        if (logger.isDebugEnabled())
        {
            logger.debug("Converting payload " + src + " to " + String.class);
        }

        String returnValue;
        Class et = getJsonConfig().getEnclosedType();
        try
        {
            if (et == null)
            {
                getJsonConfig().setEnclosedType(src.getClass());
            }

            returnValue = JsonUtils.convertJavaObjectToJson(src, getJsonConfig());
        }
        finally
        {
            if (et == null)
            {
                getJsonConfig().setEnclosedType(null);
            }
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Successfully converted to value: " + returnValue);
        }

        return returnValue;
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

    public JsonConfig getJsonConfig()
    {
        if (jsonConfig == null)
        {
            setJsonConfig(new JsonConfig());
        }
        return jsonConfig;
    }

    public void setJsonConfig(JsonConfig jsonConfig)
    {
        this.jsonConfig = jsonConfig;
    }

    public String getExcludeProperties()
    {
        return excludeProperties;
    }

    public void setExcludeProperties(String excludeProperties)
    {
        this.excludeProperties = excludeProperties;
    }

    public String getIncludeProperties()
    {
        return includeProperties;
    }

    public void setIncludeProperties(String includeProperties)
    {
        this.includeProperties = includeProperties;
    }

    public Class getSourceClass()
    {
        return sourceClass;
    }

    public void setSourceClass(Class sourceClass)
    {
        this.sourceClass = sourceClass;
    }

    private class IncludePropertiesFilter implements PropertyFilter
    {
        private String[] includedProperties;

        private IncludePropertiesFilter(String[] includedProperties)
        {
            this.includedProperties = includedProperties;
        }

        public boolean apply(Object source, String name, Object value)
        {
            for (int i = 0; i < includedProperties.length; i++)
            {
                if (includedProperties[i].equals(name))
                {
                    return false;
                }
            }
            return true;
        }
    }
}

