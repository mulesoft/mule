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
        interceptor.after(operationContext, null);
        verify(operationContext).removeVariable(CONNECTION_PARAM);
    }

    //TODO: MULE-8952 -> test these cases on the connection service

    //@Test
    //public void connectionProviderFailsToDisconnect() throws Exception
    //{
    //    final Exception exception = setconnectionProviderWhichFailsToDisconnect();
    //
    //    //have a connection established
    //    getConnection();
    //
    //    try
    //    {
    //        interceptor.stop();
    //        fail("was expecting a failure");
    //    }
    //    catch (Exception e)
    //    {
    //        assertThat(e, is(sameInstance(exception)));
    //    }
    //
    //    verify(connectionProvider).stop();
    //
    //    //stop again to verify that the write lock was released. If it didn't then this will hang
    //    //and the test will time out
    //    interceptor.stop();
    //}
    //
    //@Test
    //public void connectionProviderFailsToStop() throws Exception
    //{
    //    final Exception exception = setconnectionProviderWhichFailsToStop();
    //
    //    try
    //    {
    //        interceptor.stop();
    //        fail("was expecting a failure");
    //    }
    //    catch (Exception e)
    //    {
    //        assertThat(e, is(sameInstance(exception)));
    //    }
    //
    //    verify(connectionProvider).stop();
    //
    //    //stop again to verify that the write lock was released. If it didn't then this will hang
    //    //and the test will time out
    //    try
    //    {
    //        interceptor.stop();
    //        fail("re stopping should fail");
    //    }
    //    catch (Exception e)
    //    {
    //        // expected
    //    }
    //}

    //private Exception setconnectionProviderWhichFailsToDisconnect() throws Exception
    //{
    //    connectionProvider = mock(PetStoreConnectionProvider.class);
    //    final Exception exception = new RuntimeException();
    //    doThrow(exception).when(connectionProvider).disconnect(Mockito.any(PetStoreClient.class));
    //    when(connectionProvider.connect(config)).thenReturn(mock(PetStoreClient.class));
    //    before();
    //
    //    return exception;
    //}
    //
    //private Exception setconnectionProviderWhichFailsToStop() throws Exception
    //{
    //    connectionProvider = mock(PetStoreConnectionProvider.class);
    //    final Exception exception = new RuntimeException();
    //    doThrow(exception).when(connectionProvider).stop();
    //    before();
    //
    //    return exception;
    //}


    private PetStoreClient getConnection() throws Exception
    {
        ArgumentCaptor<PetStoreClient> connectionCaptor = ArgumentCaptor.forClass(PetStoreClient.class);
        interceptor.before(operationContext);

        verify(operationContext, atLeastOnce()).setVariable(same(CONNECTION_PARAM), connectionCaptor.capture());
        return connectionCaptor.getValue();
    }
}
