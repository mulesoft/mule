/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.types;

import org.mule.api.transformer.DataType;
import org.mule.util.generics.GenericsUtils;
import org.mule.util.generics.MethodParameter;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * A data type that represents a generified collection.  When checked for compatability both the colection type and the
 * generic item type will be compared.
 *
 * @since 3.0
 */
public class CollectionDataType<T> extends SimpleDataType<T>
{
    private Class<? extends Collection> collectionType;

    /**
     * Creates an untyped collection data type
     *
     * @param collectionType the collection class type
     */
    public CollectionDataType(Class<? extends Collection> collectionType)
    {
        super(Object.class);
        this.collectionType = collectionType;
    }

    public CollectionDataType(Class<? extends Collection> collectionType, String mimeType)
    {
        super(Object.class, mimeType);
        this.collectionType = collectionType;
    }

    public CollectionDataType(Class<? extends Collection> collectionType, Class type, String mimeType)
    {
        super(type, mimeType);
        this.collectionType = collectionType;
    }

    public CollectionDataType(Class<? extends Collection> collectionType, Class type)
    {
        super(type);
        this.collectionType = collectionType;
    }

    public Class<?> getItemType()
    {
        return type;
    }

    @Override
    public Class getType()
    {
        return collectionType;
    }

    public static CollectionDataType createFromMethodReturn(Method m)
    {
        return createFromMethodReturn(m, null);
    }

    public static CollectionDataType createFromMethodReturn(Method m, String mimeType)
    {
        Class collType = GenericsUtils.getCollectionReturnType(m);

        if (collType != null)
        {
            return new CollectionDataType(m.getReturnType(), collType, mimeType);
        }
        else
        {
            throw new IllegalArgumentException("Return type for method is not a generic type collection. " + m);
        }
    }

    public static CollectionDataType createFromMethodParam(Method m, int paramIndex)
    {
        return createFromMethodParam(m, paramIndex, null);
    }

    public static CollectionDataType createFromMethodParam(Method m, int paramIndex, String mimeType)
    {
        Class collType = GenericsUtils.getCollectionParameterType(new MethodParameter(m, paramIndex));

        if (collType != null)
        {
            return new CollectionDataType(m.getParameterTypes()[paramIndex], collType, mimeType);
        }
        else
        {
            throw new IllegalArgumentException("Parameter type (index: " + paramIndex + ") for method is not a generic type collection. " + m);
        }
    }

    public static boolean isReturnTypeACollection(Method m)
    {
        return GenericsUtils.getCollectionReturnType(m) != null;
    }

    public static boolean isParamTypeACollection(Method m, int paramIndex)
    {
        return GenericsUtils.getCollectionParameterType(new MethodParameter(m, paramIndex)) != null;
    }

    @Override
    public boolean isCompatibleWith(DataType dataType)
    {
        if (dataType instanceof ImmutableDataType)
        {
            dataType = ((ImmutableDataType)dataType).getWrappedDataType();
        }
        if (!(dataType instanceof CollectionDataType))
        {
            return false;
        }

        if (!super.isCompatibleWith(dataType))
        {
            return false;
        }
        CollectionDataType that = (CollectionDataType) dataType;

        //Untyped compatible collection
        return that.getItemType() == Object.class || this.getItemType().isAssignableFrom(that.getItemType());

    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        CollectionDataType that = (CollectionDataType) o;

        if (!getItemType().equals(that.getItemType()))
        {
            return false;
        }

        if ((mimeType != null ? !mimeType.equals(that.mimeType) : that.mimeType != null) && !ANY_MIME_TYPE.equals(that.mimeType) && !ANY_MIME_TYPE.equals(this.mimeType))
        {
            return false;
        }

        return getType().equals(that.getType());

    }

    @Override
    public int hashCode()
    {
        int result = getType().hashCode();
        result = 31 * result + getItemType().hashCode();
        result = 31 * result + (getMimeType() != null ? getMimeType().hashCode() : 0);
        result = 31 * result + (getEncoding() != null ? getEncoding().hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "CollectionDataType{" +
                "type=" + getType().getName() +
                ", itemType=" + getItemType().getName() +
                ", mimeType='" + getMimeType() + '\'' +
                '}';
    }
}
