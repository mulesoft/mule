/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.test.util;

/**
 * Defines a column for testing purposes
 */
public class ColumnMetadata
{

    private final String name;
    private final String label;
    private final int index;

    public ColumnMetadata(String name, int index)
    {
        this(name, name, index);
    }

    public ColumnMetadata(String name, String label, int index)
    {
        this.name = name;
        this.index = index;
        this.label = label;
    }

    public int getIndex()
    {
        return index;
    }

    public String getName()
    {
        return name;
    }

    public String getLabel()
    {
        return label;
    }
}
