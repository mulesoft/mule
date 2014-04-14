/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.param;

import org.mule.module.db.internal.domain.type.DbType;

/**
 * Defines a base class for implementing different kind of { @link QueryParam}
 * classes.
 */
public class AbstractQueryParam implements QueryParam
{

    private final int index;
    private final DbType type;
    private final String name;

    public AbstractQueryParam(int index, DbType type, String name)
    {
        this.index = index;
        this.type = type;
        this.name = name;
    }

    @Override
    public int getIndex()
    {
        return index;
    }

    @Override
    public DbType getType()
    {
        return type;
    }

    @Override
    public String getName()
    {
        return name;
    }
}
