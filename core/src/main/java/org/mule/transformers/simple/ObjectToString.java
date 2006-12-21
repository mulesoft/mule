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

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * <code>ObjectToString</code> transformer is useful for debugging. It will return
 * human-readable output for various kinds of objects. Right now, it is just coded to
 * handle Map and Collection objects. Others will be added.
 * 
 * @author <a href="mailto:lajos@galatea.com">Lajos Moczar</a>
 * @version $Revision$
 */
public class ObjectToString extends AbstractTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1784548784479765795L;

    public ObjectToString()
    {
        registerSourceType(Object.class);
        setReturnClass(String.class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        String output = "";

        if (src instanceof Map)
        {
            Iterator iter = ((Map)src).entrySet().iterator();
            if (iter.hasNext())
            {
                StringBuffer b = new StringBuffer(80);
                while (iter.hasNext())
                {
                    Map.Entry e = (Map.Entry)iter.next();
                    Object key = e.getKey();
                    Object value = e.getValue();
                    b.append(key.toString()).append(':').append(value.toString());
                    if (iter.hasNext())
                    {
                        b.append('|');
                    }
                }
                output = b.toString();
            }
        }
        else if (src instanceof Collection)
        {
            Iterator iter = ((Collection)src).iterator();
            if (iter.hasNext())
            {
                StringBuffer b = new StringBuffer(80);
                while (iter.hasNext())
                {
                    b.append(iter.next().toString());
                    if (iter.hasNext())
                    {
                        b.append('|');
                    }
                }
                output = b.toString();
            }
        }
        else
        {
            output = src.toString();
        }

        return output;
    }
}
