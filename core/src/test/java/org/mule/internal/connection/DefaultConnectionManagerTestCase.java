/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.internal.connection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.connection.ConnectionException;
import org.mule.api.connection.ConnectionProvider;
import org.mule.api.connection.ManagedConnection;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefaultConnectionManagerTestCase extends AbstractMuleContextTestCase
{

    private Apple config = new Apple();

    private Banana connection = new Banana();

    @Mock
    private ConnectionProvider<Apple, Banana> connectionProvider;


    private DefaultConnectionManager connectionManager = new DefaultConnectionManager();

    @Before
    public void before() throws Exception
    {
        when(connectionProvider.connect(config)).thenReturn(connection);
        muleContext.getInjector().inject(connectionManager);
    }

    @Test
    public void getConnection() throws Exception
    {
        connectionManager.bind(config, connectionProvider);
        ManagedConnection<Banana> managedConnection = connectionManager.getConnection(config);
        assertThat(managedConnection.getConnection(), is(sameInstance(connection)));
    }

    @Test
    public void assertUnbindedConnection() throws Exception
    {
        try
        {
            connectionManager.getConnection(config);
            fail("Config was unbinded yet a connection could be obtained");
        }
        catch (ConnectionException e)
        {
            // ok!
        }
    }

    @Test
    public void stop() throws Exception
    {
        getConnection();
        connectionManager.stop();
        verifyDisconnect();
    }

    @Test
    public void unbind() throws Exception
    {
        getConnection();
        connectionManager.unbind(config);
        verifyDisconnect();
        assertUnbindedConnection();
    }

    private void verifyDisconnect()
    {
        verify(connectionProvider).disconnect(connection);
    }

    @Test(expected = IllegalStateException.class)
    public void bindWithStoppingMuleContext() throws Exception
    {
        muleContext.stop();
        connectionManager.bind(config, connectionProvider);
    }
}
