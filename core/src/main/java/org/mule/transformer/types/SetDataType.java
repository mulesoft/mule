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

import java.util.Set;

/**
 * TODO
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