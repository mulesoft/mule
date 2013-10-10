/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.config;

public class DerbyDataSourceFactoryBean extends AbstractDataSourceFactoryBean
{
    private static final String DRIVER_CLASS_NAME = "org.apache.derby.jdbc.EmbeddedDriver";
    private static final String JDBC_URL_PREFIX = "jdbc:derby:";
    private static final String MEMORY_SUB_SUBPROTOCOL = "memory";

    protected boolean create = false;
    protected String database;
    protected String subsubprotocol = MEMORY_SUB_SUBPROTOCOL;

    public DerbyDataSourceFactoryBean()
    {
        super();
        driverClassName = DRIVER_CLASS_NAME;
        updateUrl();
    }

    protected void updateUrl()
    {
        StringBuilder buf = new StringBuilder(64);
        buf.append(JDBC_URL_PREFIX);
        buf.append(subsubprotocol);
        buf.append(":");
        buf.append(database);

        if (create)
        {
            buf.append(";create=true");
        }

        url = buf.toString();
    }

    public String getDatabase()
    {
        return database;
    }

    public void setDatabase(String database)
    {
        this.database = database;
        updateUrl();
    }

    public boolean isCreate()
    {
        return create;
    }

    public void setCreate(boolean create)
    {
        this.create = create;
        updateUrl();
    }
}
