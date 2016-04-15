/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.domain.database;

import org.mule.module.db.internal.domain.database.GenericDbConfigFactory;
import org.mule.module.db.internal.resolver.database.DbConfigResolver;

/**
 * Creates {@link DbConfigResolver} for MySql databases
 */
public class MySqlConfigResolverFactoryBean extends AbstractVendorConfigResolverFactoryBean
{

    private static final String DRIVER_CLASS_NAME = "com.mysql.jdbc.Driver";
    private static final String MYSQL_URL_PREFIX = "jdbc:mysql://";

    public MySqlConfigResolverFactoryBean()
    {
        super(MYSQL_URL_PREFIX, new GenericDbConfigFactory());

        setDriverClassName(DRIVER_CLASS_NAME);
    }
}