/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.types;

import java.util.List;

/**
 * Defines a List collection type with item type information
 *
 * @since 3.0
 */
public class ListDataType<L extends List<T>, T> extends CollectionDataType<L, T>
{
    private static final long serialVersionUID = 6547019606739446616L;

    protected static <L> Class<L> getListClass()
    {
        return (Class<L>) List.class;
    }

    public ListDataType()
    {
        super(getListClass());
    }

    public ListDataType(Class<T> type, String mimeType, String encoding)
    {
        super(getListClass(), type, mimeType, encoding);
    }

    public ListDataType(Class<T> type, String mimeType)
    {
        super(getListClass(), type, mimeType);
    }

    public ListDataType(Class<T> type)
    {
        super(getListClass(), type);
    }
}
