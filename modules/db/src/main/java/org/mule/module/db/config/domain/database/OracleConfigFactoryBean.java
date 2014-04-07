/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.config.domain.database;

import org.mule.module.db.domain.database.GenericDbConfig;
import org.mule.module.db.domain.type.DbTypeManager;
import org.mule.module.db.vendor.oracle.domain.config.OracleDbConfig;

import javax.sql.DataSource;

public class OracleConfigFactoryBean extends DbConfigFactoryBean
{

    private static final String DRIVER_CLASS_NAME = "oracle.jdbc.driver.OracleDriver";

    protected OracleConfigFactoryBean()
    {
        super();
        setDriverClassName(DRIVER_CLASS_NAME);
    }

    @Override
    public Class<?> getObjectType()
    {
        return OracleDbConfig.class;
    }

    @Override
    protected GenericDbConfig doCreateDbConfig(DataSource datasource, DbTypeManager dbTypeManager)
    {
        return new OracleDbConfig(datasource, getName(), dbTypeManager);
    }
}
