/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.types;

import java.util.List;

/**
 * Defines a List collection type with item type information
 *
 * @since 3.0
 */
public class ListDataType<T> extends CollectionDataType<T>
{
    public ListDataType()
    {
        super(List.class);
    }

    public ListDataType(Class type, String mimeType)
    {
        super(List.class, type, mimeType);
    }

    public ListDataType(Class type)
    {
        super(List.class, type);
    }
}
