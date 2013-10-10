/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc.config;

public class PostgresqlDataSourceFactoryBean extends AbstractHostPortDatabaseDataSourceFactoryBean
{
    private static final String DRIVER_CLASS_NAME = "org.postgresql.Driver";
    private static final String JDBC_URL_PREFIX = "jdbc:postgresql://";

    public PostgresqlDataSourceFactoryBean()
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
