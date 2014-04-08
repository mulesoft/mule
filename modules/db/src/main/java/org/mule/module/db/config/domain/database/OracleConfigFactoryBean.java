/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.config.domain.database;

import org.mule.module.db.domain.type.DbType;
import org.mule.module.db.domain.type.ResolvedDbType;

import java.util.ArrayList;
import java.util.List;

public class OracleConfigFactoryBean extends AbstractVendorConfigFactoryBean
{

    private static final String DRIVER_CLASS_NAME = "oracle.jdbc.driver.OracleDriver";
    private static final String ORACLE_URL_PREFIX = "jdbc:oracle:thin:@";
    public static final int CURSOR_TYPE_ID = -10;
    public static final String CURSOR_TYPE_NAME = "CURSOR";

    protected OracleConfigFactoryBean()
    {
        super(ORACLE_URL_PREFIX);
        setDriverClassName(DRIVER_CLASS_NAME);
    }

    @Override
    protected List<DbType> getVendorDataTypes()
    {
        List<DbType> dbTypes = new ArrayList<DbType>();
        dbTypes.add(new ResolvedDbType(CURSOR_TYPE_ID, CURSOR_TYPE_NAME));

        return dbTypes;
    }
}
