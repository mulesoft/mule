/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
