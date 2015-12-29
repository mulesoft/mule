/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.types;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.DataType;
import org.mule.util.generics.GenericsUtils;
import org.mule.util.generics.MethodParameter;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import javax.activation.DataHandler;
import javax.activation.DataSource;

/**
 * Factory class used to create {@link DataType} objects based on the parameter types passed into
 * the factory methods.
 *
 * @since 3.0
 */
public class DataTypeFactory
{
    public static final DataType<String> TEXT_STRING = new SimpleDataType<String>(String.class, MimeTypes.TEXT);
    public static final DataType<String> XML_STRING = new SimpleDataType<String>(String.class, MimeTypes.XML);
    public static final DataType<String> JSON_STRING = new SimpleDataType<String>(String.class, MimeTypes.APPLICATION_JSON);
    public static final DataType<String> HTML_STRING = new SimpleDataType<String>(String.class, MimeTypes.HTML);
    public static final DataType<String> ATOM_STRING = new SimpleDataType<String>(String.class, MimeTypes.ATOM);
    public static final DataType<String> RSS_STRING = new SimpleDataType<String>(String.class, MimeTypes.RSS);

    //Common Java types
    public static final DataType<String> STRING = new SimpleDataType<String>(String.class);
    public static final DataType<String> OBJECT = new SimpleDataType<String>(Object.class);
    public static final DataType<String> BYTE_ARRAY = new SimpleDataType<String>(byte[].class);
    public static final DataType<String> INPUT_STREAM = new SimpleDataType<String>(InputStream.class);
    public static final DataType<String> MULE_MESSAGE = new SimpleDataType<String>(MuleMessage.class);

    public static <T> DataType<T> create(Class<T> type)
    {
        return create(type, MimeTypes.ANY);
    }

    public static <T> DataType<T> createImmutable(Class<T> type)
    {
        return new ImmutableDataType<T>(create(type, MimeTypes.ANY));
    }

    public static <T> DataType<T> createWithEncoding(Class<T> type, String encoding)
    {
        DataType<T> dataType = create(type);
        dataType.setEncoding(encoding);
        return dataType;
    }

    public static <T> DataType<T> create(Class<T> type, String mimeType)
    {
        if (Collection.class.isAssignableFrom(type))
        {
            Class<? extends Collection<?>> collectionType = (Class<? extends Collection<?>>)type;
            Class<?> itemType = GenericsUtils.getCollectionType(collectionType);
            if (itemType == null)
            {
                return new CollectionDataType(collectionType, mimeType);
            }
            else
            {
                return new CollectionDataType(collectionType, itemType, mimeType);
            }
        }

        // Special case where proxies are used for testing
        if (isProxyClass(type))
        {
            return new SimpleDataType<T>(type.getInterfaces()[0], mimeType);
        }

        return new SimpleDataType<T>(type, mimeType);
    }

    public static <T> DataType create(Class<? extends Collection> collClass, Class<T> itemType)
    {
        return create(collClass, itemType, null);
    }

    public static <T> DataType create(Class<? extends Collection> collClass, Class<T> itemType, String mimeType)
    {
        return new CollectionDataType(collClass, itemType, mimeType);
    }

    /**
     * Will create a {@link org.mule.api.transformer.DataType} object from an object instance. This method will check
     * if the object value is a {@link org.mule.api.MuleMessage} instance and will take the type from the message payload
     * and check if a mime type is set on the message and used that when constructing the {@link org.mule.api.transformer.DataType}
     * object.
     *
     * @param value an object instance.  This can be a {@link org.mule.api.MuleMessage}, a collection, a proxy instance or any other
     *          object
     * @return a data type that represents the object type.
     */
    public static DataType<?> createFromObject(Object value)
    {
        if (value instanceof DataType)
        {
            return (DataType<?>) value;
        }

        Class<?> type = getObjectType(value);
        String mime = getObjectMimeType(value);

        return create(type, mime);
    }

    private static String getObjectMimeType(Object value)
    {
        String mime = null;
        if (value instanceof MuleMessage)
        {
            MuleMessage mm = (MuleMessage) value;
            mime = mm.getDataType().getMimeType();
        }
        else if (value instanceof DataHandler)
        {
            mime = ((DataHandler) value).getContentType();
        }
        else if (value instanceof DataSource)
        {
            mime = ((DataSource) value).getContentType();
        }

        if (mime != null)
        {
            int i = mime.indexOf(";");
            mime = (i > -1 ? mime.substring(0, i) : mime);
            //TODO set the charset on the DataType when the field is introduced BL-140
        }
        else
        {
            mime = MimeTypes.ANY;
        }

        return mime;
    }

    private static Class<?> getObjectType(Object value)
    {
        Class<?> type;
        if (value == null)
        {
            type = Object.class;
        }
        else
        {
            if (value instanceof MuleMessage)
            {
                MuleMessage mm = (MuleMessage) value;
                type = mm.getPayload().getClass();
            }
            else
            {
                type = value.getClass();
            }
        }
        return type;
    }

    public static DataType<?> createFromReturnType(Method m)
    {
        return createFromReturnType(m, null);
    }

    public static DataType<?> createFromReturnType(Method m, String mimeType)
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

    public static DataType createFromParameterType(Method m, int paramIndex)
    {
        return createFromParameterType(m, paramIndex, null);
    }

    public static DataType createFromParameterType(Method m, int paramIndex, String mimeType)
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

    public static DataType<?> createFromField(Field f)
    {
        return createFromField(f, null);
    }

    public static DataType<?> createFromField(Field f, String mimeType)
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


    private static ConcurrentHashMap proxyClassCache = new ConcurrentHashMap();
    /**
     * Cache which classes are proxies.  Very experimental
     */
    protected static<T> boolean isProxyClass(Class<T> type)
    {
        /**
         * map value
         */
        class ProxyIndicator
        {
            private final WeakReference<Class> targetClassRef;
            private final boolean isProxy;

            ProxyIndicator(Class targetClass, boolean proxy)
            {
                this.targetClassRef = new WeakReference<Class>(targetClass);
                isProxy = proxy;
            }

            public Class getTargetClass()
            {
                return targetClassRef.get();
            }

            public boolean isProxy()
            {
                return isProxy;
            }
        }

        String typeName = type.getName();
        ProxyIndicator indicator = (ProxyIndicator) proxyClassCache.get(typeName);
        if (indicator != null)
        {
            Class classInMap = indicator.getTargetClass();
            if (classInMap == type)
            {
                return indicator.isProxy();
            }
            else if (classInMap != null)
            {
                // We have duplicate class names from different active classloaders.  Skip the optimization for this one
                return Proxy.isProxyClass(type);
            }
        }
        // Either there's no indicator in the map or there's one that is due to be replaced
        boolean isProxy = Proxy.isProxyClass(type);
        proxyClassCache.put(typeName, new ProxyIndicator(type, isProxy));
        return isProxy;
    }
}
