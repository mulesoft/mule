/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.types;

import java.util.Set;

/**
 * Defines a Set collection type with item type information
 *
 * @since 3.0
 */
public class SetDataType<T> extends CollectionDataType<T>
{
    public SetDataType()
    {
        super(Set.class);
    }

    public SetDataType(Class type, String mimeType)
    {
        super(Set.class, type, mimeType);
    }

    public SetDataType(Class type)
    {
        super(Set.class, type);
    }
}
