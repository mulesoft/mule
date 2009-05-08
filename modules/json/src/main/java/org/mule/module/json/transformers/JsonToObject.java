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

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.json.util.JsonUtils;
import org.mule.transformer.AbstractTransformer;
import org.mule.message.DefaultMuleMessageDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.JsonConfig;

import org.apache.commons.beanutils.DynaBean;

/**
 * A transformer that will convert a JSON encoded object graph to a java object. The object type is
 * determined by the 'returnClass' attribute. Note that this transformers supports Arrays and Lists. For
 * example, to
 * convert a JSON string to an array of org.foo.Person, set the the returnClass=[Lorg.foo.Person;.
 * <p/>
 * The JSON engine can be configured using the jsonConfig attribute. This is an object reference to an
 * instance of: {@link net.sf.json.JsonConfig}. This can be created as a spring bean.
 */
public class JsonToObject extends AbstractTransformer
{
    protected JsonConfig jsonConfig;

    protected Map dtoMappings;

    public JsonToObject()
    {
        this.registerSourceType(JSONObject.class);
        this.registerSourceType(String.class);
        this.registerSourceType(byte[].class);
        setReturnClass(Object.class);
        dtoMappings = new HashMap(1);
        dtoMappings.put("payload", HashMap.class);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        if (getReturnClass().equals(Object.class))
        {
            logger.warn("The return class is not set not type validation will be done");
        }
        if (getReturnClass().isArray())
        {
            getJsonConfig().setEnclosedType(getReturnClass());
            getJsonConfig().setArrayMode(JsonConfig.MODE_OBJECT_ARRAY);
        }
        else if (List.class.isAssignableFrom(getReturnClass()))
        {
            getJsonConfig().setEnclosedType(getReturnClass());
            getJsonConfig().setArrayMode(JsonConfig.MODE_LIST);
        }
        else if (Set.class.isAssignableFrom(getReturnClass()))
        {
            getJsonConfig().setEnclosedType(getReturnClass());
            getJsonConfig().setArrayMode(JsonConfig.MODE_SET);
        }
    }

    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        try
        {
            Object returnValue = null;

            if (src instanceof byte[])
            {
                src = new String((byte[]) src, encoding);
            }


            if (src instanceof String)
            {
                if (getReturnClass().equals(DynaBean.class))
                {
                    JSON json = JSONSerializer.toJSON(src.toString(), getJsonConfig());
                    returnValue = JSONObject.toBean((JSONObject) json, getJsonConfig());
                }
                else
                {
                    returnValue = JsonUtils.convertJsonToBean((String) src, getJsonConfig(), getReturnClass(),
                            (getReturnClass().equals(DefaultMuleMessageDTO.class) ? dtoMappings : null));
                }
            }
            else if (src instanceof JSONObject)
            {
                returnValue = JSONObject.toBean((JSONObject) src, getReturnClass(), new HashMap());
            }

            return returnValue;
        }
        catch (Exception e)
        {
            throw new TransformerException(CoreMessages.transformFailed("json", getReturnClass().getName()), this, e);
        }
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
}
