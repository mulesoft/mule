/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.types;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.DataType;
import org.mule.util.generics.GenericsUtils;
import org.mule.util.generics.MethodParameter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;

/**
 * Factory class used to create {@link org.mule.api.transformer.DataType} objects based on the parameter types passed into the
 * factory methods.
 */
public class DataTypeFactory
{
    public DataType create(Class type)
    {
        return create(type, (String) null);
    }

    public DataType create(Class type, String mimeType)
    {
        if (Collection.class.isAssignableFrom(type))
        {
            Class<? extends Collection> cType = (Class<? extends Collection>) type;
            Class itemType = GenericsUtils.getCollectionType(cType);
            if (itemType == null)
            {
                return new CollectionDataType(cType, mimeType);
            }
            else
            {
                return new CollectionDataType(cType, itemType, mimeType);
            }
        }
        //Special case where proxies are used for testing
        if (Proxy.isProxyClass(type))
        {
            type = type.getInterfaces()[0];
        }
        return new SimpleDataType(type, mimeType);
    }

    public DataType create(Class<? extends Collection> collClass, Class itemType)
    {
        return create(collClass, itemType, null);
    }

    public DataType create(Class<? extends Collection> collClass, Class itemType, String mimeType)
    {
        return new CollectionDataType(collClass, itemType, mimeType);
    }

    /**
     * Will create a {@link org.mule.api.transformer.DataType} object from an object instance. This method will check
     * if the object o is a {@link org.mule.api.MuleMessage} instance and will take the type from the message payload
     * and check if a mime type is set on the message and used that when constructing the {@link org.mule.api.transformer.DataType}
     * object.
     *
     * @param o an object instance.  This can be a {@link org.mule.api.MuleMessage}, a collection, a proxy instance or any other
     *          object
     * @return a data type that represents the object type.
     */
    public DataType createFromObject(Object o)
    {
        Class type = o.getClass();
        String mime = null;
        if (o instanceof MuleMessage)
        {
            MuleMessage mm = (MuleMessage) o;
            type = mm.getPayload().getClass();
            //TODO better mime handling, see MULE-4639
//            mime = mm.getStringProperty("Content-Type", null);
//            if(mime!=null)
//            {
//                int i = mime.indexOf(";");
//                mime = (i >-1 ? mime.substring(0, i) : mime);
//            }
        }
        return create(type, mime);
    }

    public DataType createFromReturnType(Method m)
    {
        return createFromReturnType(m, null);
    }

    public DataType createFromReturnType(Method m, String mimeType)
    {
        if (Collection.class.isAssignableFrom(m.getReturnType()))
        {
            Class<? extends Collection> cType = (Class<? extends Collection>) m.getReturnType();
            Class itemType = GenericsUtils.getCollectionReturnType(m);

            if (itemType != null)
            {
                return new CollectionDataType(cType, itemType, mimeType);
            }
            else
            {
                return new CollectionDataType(cType, mimeType);
            }
        }
        else
        {
            return new SimpleDataType(m.getReturnType(), mimeType);
        }
    }

    public DataType createFromParameterType(Method m, int paramIndex)
    {
        return createFromParameterType(m, paramIndex, null);
    }

    public DataType createFromParameterType(Method m, int paramIndex, String mimeType)
    {
        if (Collection.class.isAssignableFrom(m.getParameterTypes()[paramIndex]))
        {
            Class<? extends Collection> cType = (Class<? extends Collection>) m.getParameterTypes()[paramIndex];
            Class itemType = GenericsUtils.getCollectionParameterType(new MethodParameter(m, paramIndex));

            if (itemType != null)
            {
                return new CollectionDataType(cType, itemType, mimeType);
            }
            else
            {
                return new CollectionDataType(cType, mimeType);
            }
        }
        else
        {
            return new SimpleDataType(m.getParameterTypes()[paramIndex], mimeType);
        }
    }


    public DataType createFromField(Field f)
    {
        return createFromField(f, null);
    }

    public DataType createFromField(Field f, String mimeType)
    {
        if (Collection.class.isAssignableFrom(f.getType()))
        {
            Class<? extends Collection> cType = (Class<? extends Collection>) f.getType();
            Class itemType = GenericsUtils.getCollectionFieldType(f);

            if (itemType != null)
            {
                return new CollectionDataType(cType, itemType, mimeType);
            }
            else
            {
                return new CollectionDataType(cType, mimeType);
            }
        }
        else
        {
            return new SimpleDataType(f.getType(), mimeType);
        }
    }

}
