/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.test.util;

import org.mule.module.db.internal.domain.type.JdbcTypes;

/**
 * Type definitions for testing purposes
 */
public final class TestDbTypeMetadata
{

    public static final TypeMetadata INTEGER_DB_TYPE_METADATA = new TypeMetadata(JdbcTypes.INTEGER_DB_TYPE.getName(), JdbcTypes.INTEGER_DB_TYPE.getId());
}
