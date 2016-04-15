/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.query;

/**
 * Defines types of SQL queries
 */
public enum QueryType
{

    SELECT,

    UPDATE,

    INSERT,

    DELETE,

    TRUNCATE,

    MERGE,

    STORE_PROCEDURE_CALL,

    DDL
}
