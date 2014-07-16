/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.MessageFactory;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

import java.util.Map;

/**
 * <code>MapLookup</code> looks up and returns an object from a Map based on a key.
 */

public class MapLookup extends AbstractTransformer
{

    protected volatile Object key;

    public MapLookup()
    {
        registerSourceType(DataTypeFactory.create(Map.class));
        setReturnDataType(DataTypeFactory.OBJECT);
    }

    @Override
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        if (src instanceof Map)
        {
            if (key != null)
            {
                return ((Map) src).get(key);
            }
            else
            {
                throw new TransformerException(MessageFactory
                        .createStaticMessage("Property 'key' must be set in order to use this transformer."));
            }
        }
        else
        {
            throw new TransformerException(MessageFactory
                    .createStaticMessage("Message to transform must be of type java.util.Map"));
        }
    }

    public Object getKey()
    {
        return key;
    }

    public void setKey(Object key)
    {
        this.key = key;
    }

}
