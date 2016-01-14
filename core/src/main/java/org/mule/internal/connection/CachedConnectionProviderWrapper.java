/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.internal.connection;

import org.mule.api.connection.ConnectionProvider;
import org.mule.api.connection.ConnectionValidationResult;

/**
 * A {@link ConnectionProviderWrapper} which decorates the {@link #delegate}
 * with a user configured {@link #disableValidation} flag value.
 * <p/>
 * The purpose of this class is, in case of a {@link #disableValidation} with {@code true} as value,
 * is to delegate the validation the actual connection to the {@link #delegate}. If {@link #disableValidation} is {@code false},
 * the validation will return a {@link ConnectionValidationResult} with a valid status.
 *
 * @since 4.0
 */
public final class CachedConnectionProviderWrapper<Config, Connection> extends ConnectionProviderWrapper<Config, Connection>
{

    private boolean disableValidation;

    public CachedConnectionProviderWrapper(ConnectionProvider<Config, Connection> provider, boolean disableValidation)
    {
        super(provider);
        this.disableValidation = disableValidation;
    }

    @Override
    public ConnectionValidationResult validate(Connection connection)
    {
        if (disableValidation)
        {
            return ConnectionValidationResult.success();
        }
        return getDelegate().validate(connection);
    }
}

