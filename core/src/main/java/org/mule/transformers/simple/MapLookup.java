/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.simple;

import java.util.Map;

import org.mule.config.i18n.Message;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

/**
 * <code>MapLookup</code> looks up and returns an object from a Map based on a key.
 * 
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */

public class MapLookup extends AbstractTransformer
{

    private static final long serialVersionUID = -9033005899991305309L;

    protected Object key;

    public MapLookup()
    {
        registerSourceType(Map.class);
        setReturnClass(Object.class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        if (src instanceof Map)
        {
            if (key != null)
            {
                return ((Map)src).get(key);
            }
            else
            {
                throw new TransformerException(
                    Message.createStaticMessage("Property 'key' must be set in order to use this transformer."));
            }
        }
        else
            throw new TransformerException(
                Message.createStaticMessage("Message to transform must be of type java.util.Map"));
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
