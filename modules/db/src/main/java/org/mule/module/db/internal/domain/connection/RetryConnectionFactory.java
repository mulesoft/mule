/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.connection;

import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryPolicyTemplate;

import java.sql.Connection;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

/**
 * Defines a {@link ConnectionFactory} that is configured to retry connection
 * creation in case of errors using a {@link RetryPolicyTemplate}
 */
public class RetryConnectionFactory extends AbstractConnectionFactory
{

    private final RetryPolicyTemplate retryPolicyTemplate;
    private final ConnectionFactory delegate;

    public RetryConnectionFactory(RetryPolicyTemplate retryPolicyTemplate, ConnectionFactory delegate)
    {
        this.retryPolicyTemplate = retryPolicyTemplate;
        this.delegate = delegate;

        if (!retryPolicyTemplate.isSynchronous())
        {
            throw new IllegalArgumentException("This component doesn't support asynchronous retry policies.");
        }
    }

    @Override
    protected Connection doCreateConnection(final DataSource dataSource)
    {
        final AtomicReference<Connection> connectionRef = new AtomicReference<Connection>();

        try
        {
            retryPolicyTemplate.execute(new RetryCallback()
            {
                public void doWork(RetryContext context) throws Exception
                {
                    Connection connection = delegate.create(dataSource);

                    connectionRef.set(connection);
                }

                public String getWorkDescription()
                {
                    return "Connection factory";
                }

                @Override
                public Object getWorkOwner()
                {
                    return delegate;
                }
            }, null);
        }
        catch (Exception e)
        {
            throw new ConnectionCreationException(e);
        }

        return connectionRef.get();
    }

}
