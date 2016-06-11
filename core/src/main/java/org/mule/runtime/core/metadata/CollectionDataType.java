/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.metadata;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MimeType;

import java.util.Collection;

/**
 * A data type that represents a generified collection.
 * <p>
 * When checked for compatibility both the collection type and the generic item type will be
 * compared.
 *
 * @since 3.0
 */
public class CollectionDataType<C extends Collection<T>, T> extends SimpleDataType<C>
{
    private static final long serialVersionUID = 3600944898597616006L;

    private final Class<T> itemsType;

    CollectionDataType(Class<C> collectionType, Class<T> type, String mimeType, String encoding)
    {
        super(collectionType, mimeType, encoding);
        this.itemsType = type;
    }

    public Class<T> getItemType()
    {
        return itemsType;
    }

    @Override
    public boolean isCompatibleWith(DataType dataType)
    {
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

        if ((mimeType != null ? !mimeType.equals(that.mimeType) : that.mimeType != null) && !MimeType.ANY.equals(that.mimeType) && !MimeType.ANY.equals(this.mimeType))
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
