/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.config;

public class Db2DatasourceFactoryBean extends AbstractHostPortDatabaseDataSourceFactoryBean
{
    private static final String DRIVER_CLASS_NAME = "com.ibm.db2.jcc.DB2Driver";
    private static final String JDBC_URL_PREFIX = "jdbc:db2://";

    public Db2DatasourceFactoryBean()
    {
        super();
        driverClassName = DRIVER_CLASS_NAME;
        updateUrl();
    }

    @Override
    protected String getJdbcUrlPrefix()
    {
        return JDBC_URL_PREFIX;
    }
}
