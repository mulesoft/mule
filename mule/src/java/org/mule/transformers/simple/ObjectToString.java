/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Lajos Moczar. All rights reserved.
 * http://www.galatea.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.transformers.simple;

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * <code>ObjectToString</code> transformer is useful for debugging.
 * It will return human-readable output for various kinds of objects.
 * Right now, it is just coded to handle Map and Collection objects.
 * Others will be added.
 * 
 * @author <a href="mailto:lajos@galatea.com">Lajos Moczar</a>
 * @version $Revision$
 */
public class ObjectToString extends AbstractTransformer
{
    public ObjectToString()
    {
        registerSourceType(Object.class);
        setReturnClass(String.class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
	String output = "";
        if (src instanceof Map) {
	    Map map = (Map)src;
	    Iterator iter = map.keySet().iterator();
	    while (iter.hasNext()) {
		Object key = iter.next();
		Object value = map.get(key);
		output += key.toString() + ":" + value.toString() + "|";
	    }
        } else if (src instanceof Collection) {
	    Collection coll = (Collection)src;
	    Object[] objs = coll.toArray();
	    
	    for (int i = 0; i < objs.length; i++) {
		output += objs[i].toString() + "|";
	    }
	} else {
	    output = src.toString();
	}

	return output;
    }
}

