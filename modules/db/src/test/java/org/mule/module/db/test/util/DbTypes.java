/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.test.util;

import org.mule.module.db.domain.type.DbType;
import org.mule.module.db.domain.type.ResolvedDbType;

import java.sql.Types;

/**
 * Type definitions for testing purposes
 */
public final class DbTypes
{

    private static final String INTEGER_NAME = "INTEGER";

    public static final DbType INTEGER_DB_TYPE = new ResolvedDbType(Types.INTEGER, INTEGER_NAME);

    public static final TypeMetadata INTEGER_DB_TYPE_METADATA = new TypeMetadata(INTEGER_NAME, Types.INTEGER);
}
