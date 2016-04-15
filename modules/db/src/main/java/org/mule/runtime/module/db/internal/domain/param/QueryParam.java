/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.param;

import org.mule.module.db.internal.domain.type.DbType;

/**
 * Defines common SQL parameter information.
 */
public interface QueryParam
{

    /**
     * Returns the parameter's index in the containing SQL query.
     *
     * @return the index. Must be greater than zero.
     */
    int getIndex();

    /**
     * Returns the parameter's type.
     *
     * @return an integer constant that represent the type in the used JDBC provider.
     */
    DbType getType();

    /**
     * Returns the name parameter name.
     */
    String getName();

}
