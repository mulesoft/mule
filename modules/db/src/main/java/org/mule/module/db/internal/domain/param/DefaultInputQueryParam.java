/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.param;

import org.mule.module.db.internal.domain.type.DbType;

public class DefaultInputQueryParam extends AbstractQueryParam implements InputQueryParam
{

    private final Object value;

    private boolean isDbParam = false;

    public DefaultInputQueryParam(int index, DbType type, Object value)
    {
        this(index, type, value, null);
    }

    public DefaultInputQueryParam(int index, DbType type, Object value, String name)
    {
        super(index, type, name);
        this.value = value;
    }

    @Override
    public Object getValue()
    {
        return value;
    }

    /**
     * @return true if the InputQueryParam references a param that must be defined through a <db:in-param> element in the configuration file.
     * false if the InputQueryParam is defined through a literal or a MEL expression
     */
    public boolean isDbInParam()
    {
        return isDbParam;
    }

    public void setDbParam(boolean dbParam)
    {
        isDbParam = dbParam;
    }
}
