/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.types;

import java.util.Set;

/**
 * Defines a Set collection type with item type information
 *
 * @since 3.0
 */
public class SetDataType<S extends Set<T>, T> extends CollectionDataType<S, T>
{
    private static final long serialVersionUID = 4604600409023719034L;

    protected static <S> Class<S> getSetClass()
    {
        return (Class<S>) Set.class;
    }
    
    public SetDataType()
    {
        super(getSetClass());
    }

    public SetDataType(Class<T> type, String mimeType, String encoding)
    {
        super(getSetClass(), type, mimeType, encoding);
    }

    public SetDataType(Class<T> type, String mimeType)
    {
        super(getSetClass(), type, mimeType);
    }

    public SetDataType(Class<T> type)
    {
        super(getSetClass(), type);
    }
}
