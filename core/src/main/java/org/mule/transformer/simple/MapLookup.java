/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
