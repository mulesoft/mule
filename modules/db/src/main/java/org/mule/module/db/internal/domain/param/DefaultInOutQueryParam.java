/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.param;

import org.mule.module.db.internal.domain.type.DbType;

public class DefaultInOutQueryParam extends AbstractQueryParam implements InOutQueryParam
{

    private final Object value;

    public DefaultInOutQueryParam(int index, DbType type, String name, Object value)
    {
        super(index, type, name);

        this.value = value;
    }

    @Override
    public Object getValue()
    {
        return value;
    }
}
