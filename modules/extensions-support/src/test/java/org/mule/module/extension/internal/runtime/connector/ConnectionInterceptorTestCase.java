/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.connector;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.module.extension.internal.ExtensionProperties.CONNECTION_PARAM;
import org.mule.api.connection.ManagedConnection;
import org.mule.api.connector.ConnectionManager;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.module.extension.internal.runtime.connector.petstore.PetStoreClient;
import org.mule.module.extension.internal.runtime.connector.petstore.PetStoreConnectionProvider;
import org.mule.module.extension.internal.runtime.connector.petstore.PetStoreConnector;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.concurrent.Latch;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConnectionInterceptorTestCase extends AbstractMuleContextTestCase
{

    private static final String USER = "john";
    private static final String PASSWORD = "Doe";

    @Mock(answer = RETURNS_DEEP_STUBS)
    private OperationContextAdapter operationContext;

    @Mock
    private PetStoreConnector config;

    private PetStoreConnectionProvider connectionProvider = spy(new PetStoreConnectionProvider());
    private ConnectionInterceptor interceptor;

    @Before
    public void before() throws Exception
    {
        when(operationContext.getConfiguration().getValue()).thenReturn(config);
        when(operationContext.getConfiguration().getConnectionProvider()).thenReturn(Optional.of(connectionProvider));
        when(operationContext.getVariable(CONNECTION_PARAM)).thenReturn(null);

        connectionProvider.setUsername(USER);
        connectionProvider.setPassword(PASSWORD);

        ConnectionManager connectionManager = muleContext.getRegistry().lookupObject(ConnectionManager.class);
        connectionManager.bind(config, connectionProvider);

        interceptor = new ConnectionInterceptor();
        muleContext.getInjector().inject(interceptor);
    }

    @Test
    public void setConnection() throws Exception
    {
        PetStoreClient connection = getConnection();
        verify(connectionProvider).connect(config);

        assertThat(connection, is(notNullValue()));
        assertThat(connection.getUsername(), is(USER));
        assertThat(connection.getPassword(), is(PASSWORD));
    }

    @Test
    public void returnsAlwaysSameConnectionAndConnectOnlyOnce() throws Exception
    {
        PetStoreClient connection1 = getConnection();
        PetStoreClient connection2 = getConnection();

        assertThat(connection1, is(sameInstance(connection2)));
        verify(connectionProvider).connect(config);
    }

    @Test
    public void getConnectionConcurrentlyAndConnectOnlyOnce() throws Exception
    {
        PetStoreClient mockConnection = mock(PetStoreClient.class);
        connectionProvider = mock(PetStoreConnectionProvider.class);
        before();

        Latch latch = new Latch();
        when(connectionProvider.connect(config)).thenAnswer(invocation -> {
            new Thread(() -> {
                try
                {
                    latch.release();
                    getConnection();
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }).start();

            return mockConnection;
        });

        PetStoreClient connection = getConnection();
        assertThat(latch.await(5, TimeUnit.SECONDS), is(true));
        assertThat(connection, is(sameInstance(mockConnection)));
        verify(connectionProvider).connect(config);
    }

    @Test
    public void after()
    {
        ManagedConnection managedConnection = mock(ManagedConnection.class);
        when(operationContext.removeVariable(CONNECTION_PARAM)).thenReturn(managedConnection);

        interceptor.after(operationContext, null);
        verify(operationContext).removeVariable(CONNECTION_PARAM);
        verify(managedConnection).release();
    }

    private PetStoreClient getConnection() throws Exception
    {
        ArgumentCaptor<ManagedConnection> connectionCaptor = ArgumentCaptor.forClass(ManagedConnection.class);
        interceptor.before(operationContext);

        verify(operationContext, atLeastOnce()).setVariable(same(CONNECTION_PARAM), connectionCaptor.capture());
        ManagedConnection<PetStoreClient> managedConnection = connectionCaptor.getValue();
        return managedConnection.getConnection();
    }
}
