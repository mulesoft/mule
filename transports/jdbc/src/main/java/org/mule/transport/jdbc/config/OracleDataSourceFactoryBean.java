/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc.config;

public class OracleDataSourceFactoryBean extends AbstractDataSourceFactoryBean
{
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_INSTANCE = "orcl";
    private static final int DEFAULT_PORT = 1521;
    private static final String DRIVER_CLASS_NAME = "oracle.jdbc.driver.OracleDriver";
    private static final String JDBC_URL_PREFIX = "jdbc:oracle:thin:@";

    protected String host = DEFAULT_HOST;
    protected int port = DEFAULT_PORT;
    protected String instance = DEFAULT_INSTANCE;

    public OracleDataSourceFactoryBean()
    {
        super();
        driverClassName = DRIVER_CLASS_NAME;
        updateUrl();
    }

    protected void updateUrl()
    {
        StringBuilder buf = new StringBuilder(JDBC_URL_PREFIX);
        buf.append(host);
        buf.append(":");
        buf.append(port);
        buf.append(":");
        buf.append(instance);

        url = buf.toString();
    }

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
        updateUrl();
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
        updateUrl();
    }

    public String getInstance()
    {
        return instance;
    }

    public void setInstance(String instance)
    {
        this.instance = instance;
        updateUrl();
    }
}
