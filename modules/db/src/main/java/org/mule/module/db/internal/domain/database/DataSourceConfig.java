/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.database;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.context.MuleContextAware;
import org.mule.module.db.internal.domain.connection.DbPoolingProfile;
import org.mule.util.StringUtils;

/**
 * Maintains configuration information about how to build a {@link javax.sql.DataSource}
 */
public class DataSourceConfig implements MuleContextAware
{

    public static final int NO_TRANSACTION_ISOLATION_CONFIGURED = -1;

    private MuleContext muleContext;
    private String url;
    private String driverClassName;
    private int connectionTimeout;
    private String password;
    private String user;
    private int transactionIsolation = NO_TRANSACTION_ISOLATION_CONFIGURED;
    private boolean useXaTransactions;
    private DbPoolingProfile poolingProfile;

    /**
     * Resolves dataSource configuration using a given event
     *
     * @param muleEvent current event being executed. If null, original unresolved configuration is returned
     * @return a non null dataSource configuration with all dynamic attributes resolved
     */
    public DataSourceConfig resolve(MuleEvent muleEvent)
    {
        if (muleEvent == null)
        {
            return this;
        }

        DataSourceConfig resolved = new DataSourceConfig();
        resolved.setMuleContext(muleContext);
        resolved.url = resolveAttribute(url, muleEvent);
        resolved.driverClassName = resolveAttribute(driverClassName, muleEvent);
        resolved.password = resolveAttribute(password, muleEvent);
        resolved.user = resolveAttribute(user, muleEvent);
        resolved.connectionTimeout = connectionTimeout;
        resolved.transactionIsolation = transactionIsolation;
        resolved.useXaTransactions = useXaTransactions;
        resolved.poolingProfile = poolingProfile;

        return resolved;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getDriverClassName()
    {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName)
    {
        this.driverClassName = driverClassName;
    }

    public int getConnectionTimeout()
    {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout)
    {
        this.connectionTimeout = connectionTimeout;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public int getTransactionIsolation()
    {
        return transactionIsolation;
    }

    public void setTransactionIsolation(int transactionIsolation)
    {
        this.transactionIsolation = transactionIsolation;
    }

    public boolean isUseXaTransactions()
    {
        return useXaTransactions;
    }

    public void setUseXaTransactions(boolean useXaTransactions)
    {
        this.useXaTransactions = useXaTransactions;
    }

    public DbPoolingProfile getPoolingProfile()
    {
        return poolingProfile;
    }

    public void setPoolingProfile(DbPoolingProfile poolingProfile)
    {
        this.poolingProfile = poolingProfile;
    }

    public boolean isDynamic()
    {
        return isDynamicAttribute(url) || isDynamicAttribute(user) || isDynamicAttribute(password) || isDynamicAttribute(driverClassName);
    }

    private boolean isDynamicAttribute(String attribute)
    {
        return !StringUtils.isEmpty(attribute) && muleContext.getExpressionManager().isValidExpression(attribute);
    }

    private String resolveAttribute(String value, MuleEvent muleEvent)
    {
        return isDynamicAttribute(value) ? muleContext.getExpressionManager().parse(value, muleEvent) : value;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        DataSourceConfig that = (DataSourceConfig) o;

        if (connectionTimeout != that.connectionTimeout)
        {
            return false;
        }
        if (transactionIsolation != that.transactionIsolation)
        {
            return false;
        }
        if (useXaTransactions != that.useXaTransactions)
        {
            return false;
        }
        if (!url.equals(that.url))
        {
            return false;
        }
        if (!driverClassName.equals(that.driverClassName))
        {
            return false;
        }
        if (password != null ? !password.equals(that.password) : that.password != null)
        {
            return false;
        }
        if (user != null ? !user.equals(that.user) : that.user != null)
        {
            return false;
        }
        return !(poolingProfile != null ? !poolingProfile.equals(that.poolingProfile) : that.poolingProfile != null);
    }

    @Override
    public int hashCode()
    {
        int result = url.hashCode();
        result = 31 * result + driverClassName.hashCode();
        result = 31 * result + connectionTimeout;
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (user != null ? user.hashCode() : 0);
        result = 31 * result + transactionIsolation;
        result = 31 * result + (useXaTransactions ? 1 : 0);
        result = 31 * result + (poolingProfile != null ? poolingProfile.hashCode() : 0);
        return result;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }
}