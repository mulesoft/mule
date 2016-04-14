/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.internal.connection;

import org.mule.api.connection.PoolingListener;

/**
 * Implementation of the Null Object design pattern for the {@link PoolingListener} interface.
 *
 * @param <Config>     the generic type for the config object to which the connection is bound
 * @param <Connection> the generic type for the pooled connection
 * @since 4.0
 */
final class NullPoolingListener<Config, Connection> implements PoolingListener<Config, Connection>
{

    /**
     * Does nothing
     */
    @Override
    public void onBorrow(Config config, Connection connection)
    {

    }

    /**
     * Does nothing
     */
    @Override
    public void onReturn(Config config, Connection connection)
    {

    }
}
