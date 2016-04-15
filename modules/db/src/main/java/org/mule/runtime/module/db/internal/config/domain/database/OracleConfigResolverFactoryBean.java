/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.domain.database;

import org.mule.module.db.internal.domain.database.OracleDbConfigFactory;
import org.mule.module.db.internal.resolver.database.DbConfigResolver;

/**
 * Creates {@link DbConfigResolver} for Oracle databases
 */
public class OracleConfigResolverFactoryBean extends AbstractVendorConfigResolverFactoryBean
{

    private static final String DRIVER_CLASS_NAME = "oracle.jdbc.driver.OracleDriver";
    private static final String ORACLE_URL_PREFIX = "jdbc:oracle:thin:@";

    protected OracleConfigResolverFactoryBean()
    {
        super(ORACLE_URL_PREFIX, new OracleDbConfigFactory());
        setDriverClassName(DRIVER_CLASS_NAME);
    }


}
