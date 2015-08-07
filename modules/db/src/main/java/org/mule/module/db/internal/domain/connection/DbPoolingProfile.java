/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.connection;

import java.util.Objects;

/**
 * Describes a database pooling profile
 */
public class DbPoolingProfile
{

    private int maxPoolSize = 5;
    private int minPoolSize = 0;
    private int acquireIncrement = 1;
    private int preparedStatementCacheSize = 5;
    private int maxWaitMillis = 300000;  // 30 seconds

    public int getPreparedStatementCacheSize()
    {
        return preparedStatementCacheSize;
    }

    public void setPreparedStatementCacheSize(int preparedStatementCacheSize)
    {
        this.preparedStatementCacheSize = preparedStatementCacheSize;
    }

    public int getMaxPoolSize()
    {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize)
    {
        this.maxPoolSize = maxPoolSize;
    }

    public int getMinPoolSize()
    {
        return minPoolSize;
    }

    public void setMinPoolSize(int minPoolSize)
    {
        this.minPoolSize = minPoolSize;
    }

    public int getAcquireIncrement()
    {
        return acquireIncrement;
    }

    public void setAcquireIncrement(int acquireIncrement)
    {
        this.acquireIncrement = acquireIncrement;
    }

    public int getMaxWaitMillis()
    {
        return maxWaitMillis;
    }

    public void setMaxWaitMillis(int maxWaitMillis)
    {

        this.maxWaitMillis = maxWaitMillis;
    }


    @Override
    public int hashCode()
    {
        return Objects.hash(minPoolSize, maxPoolSize, acquireIncrement, preparedStatementCacheSize, maxWaitMillis);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null || getClass() != obj.getClass())
        {
            return false;
        }

        DbPoolingProfile that = (DbPoolingProfile) obj;

        if (maxPoolSize != that.maxPoolSize)
        {
            return false;
        }
        if (minPoolSize != that.minPoolSize)
        {
            return false;
        }
        if (acquireIncrement != that.acquireIncrement)
        {
            return false;
        }
        if (preparedStatementCacheSize != that.preparedStatementCacheSize)
        {
            return false;
        }
        return maxWaitMillis == that.maxWaitMillis;

    }
}
