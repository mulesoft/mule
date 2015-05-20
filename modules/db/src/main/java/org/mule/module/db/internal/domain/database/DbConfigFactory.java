/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.database;

import javax.sql.DataSource;

/**
 * Creates {@link DbConfig} instances
 */
public interface DbConfigFactory
{

    /**
     * Creates a {@link DbConfig} to access a given {@link DataSource}
     *
     * @param name name of the config
     * @param dataSource dataSource to access from the created DbConfig
     * @return a non null DbConfig
     */
    DbConfig create(String name, DataSource dataSource);
}
