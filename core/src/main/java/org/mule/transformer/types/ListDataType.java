/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
