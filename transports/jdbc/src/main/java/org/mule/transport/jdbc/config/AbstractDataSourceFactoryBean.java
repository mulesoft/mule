/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.config;

import org.enhydra.jdbc.standard.StandardDataSource;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class AbstractDataSourceFactoryBean extends AbstractFactoryBean<StandardDataSource>
{
    protected String driverClassName;
    protected int loginTimeout = -1;
    protected String password;
    protected int transactionIsolation;
    protected String url;
    protected String user;

    @Override
    public Class<?> getObjectType()
    {
        return StandardDataSource.class;
    }

    @Override
    protected StandardDataSource createInstance() throws Exception
    {
        StandardDataSource dataSource = new StandardDataSource();
        dataSource.setDriverName(driverClassName);
        if (loginTimeout >= 0)
        {
            dataSource.setLoginTimeout(loginTimeout);
        }
        dataSource.setPassword(password);
        dataSource.setTransactionIsolation(transactionIsolation);
        dataSource.setUrl(url);
        dataSource.setUser(user);
        return dataSource;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public String getUser()
    {
        return user;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getUrl()
    {
        return url;
    }

    public void setTransactionIsolation(int transactionIsolation)
    {
        this.transactionIsolation = transactionIsolation;
    }

    public int getTransactionIsolation()
    {
        return transactionIsolation;
    }

    public void setLoginTimeout(int loginTimeout)
    {
        this.loginTimeout = loginTimeout;
    }

    public int getLoginTimeout()
    {
        return loginTimeout;
    }
}
