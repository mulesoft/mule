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
import static org.junit.Assert.fail;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.module.extension.internal.ExtensionProperties.CONNECTION_PARAM;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.module.extension.internal.runtime.connector.petstore.PetStoreClient;
import org.mule.module.extension.internal.runtime.connector.petstore.PetStoreClientConnectionHandler;
import org.mule.module.extension.internal.runtime.connector.petstore.PetStoreConnectorConfig;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConnectionInterceptorTestCase extends AbstractMuleContextTestCase
{

    private static final String USER = "john";
    private static final String PASSWORD = "Doe";

    @Mock(answer = RETURNS_DEEP_STUBS)
    private OperationContextAdapter operationContext;

    @Mock
    private PetStoreConnectorConfig config;

    private ConnectionInterceptor<PetStoreConnectorConfig, PetStoreClient> interceptor;
    private PetStoreClientConnectionHandler connectionHandler = spy(new PetStoreClientConnectionHandler());

    @Before
    public void before() throws Exception
    {
        when(operationContext.getConfiguration().getValue()).thenReturn(config);
        when(operationContext.getVariable(CONNECTION_PARAM)).thenReturn(null);

        when(config.getUsername()).thenReturn(USER);
        when(config.getPassword()).thenReturn(PASSWORD);

        interceptor = new ConnectionInterceptor<>(connectionHandler);
        muleContext.getInjector().inject(interceptor);
    }

    @Test
    public void setConnection() throws Exception
    {
        PetStoreClient connection = getConnection();
        verify(connectionHandler).connect(config);

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
        verify(connectionHandler).connect(config);
    }

    @Test
    public void getConnectionConcurrentlyAndConnectOnlyOnce() throws Exception
    {
        PetStoreClient mockConnection = mock(PetStoreClient.class);
        connectionHandler = mock(PetStoreClientConnectionHandler.class);
        before();

        Latch latch = new Latch();
        when(connectionHandler.connect(config)).thenAnswer(invocation -> {
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
        verify(connectionHandler).connect(config);

    }

    @Test
    public void after()
    {
        interceptor.after(operationContext, null);
        verify(operationContext).setVariable(CONNECTION_PARAM, null);
    }

    @Test
    public void initialise() throws Exception
    {
        interceptor.initialise();
        assertThat(connectionHandler.getMuleContext(), is(sameInstance(muleContext)));
        assertThat(connectionHandler.getInitialise(), is(1));
    }

    @Test
    public void start() throws Exception
    {
        interceptor.start();
        assertThat(connectionHandler.getStart(), is(1));
    }

    @Test
    public void stop() throws Exception
    {
        interceptor.stop();
        assertThat(connectionHandler.getStop(), is(1));
    }

    @Test
    public void connectionHandlerFailsToDisconnect() throws Exception
    {
        final Exception exception = setConnectionHandlerWhichFailsToDisconnect();

        //have a connection established
        getConnection();

        try
        {
            interceptor.stop();
            fail("was expecting a failure");
        }
        catch (Exception e)
        {
            assertThat(e, is(sameInstance(exception)));
        }

        verify(connectionHandler).stop();

        //stop again to verify that the write lock was released. If it didn't then this will hang
        //and the test will time out
        interceptor.stop();
    }

    @Test
    public void connectionHandlerFailsToStop() throws Exception
    {
        final Exception exception = setConnectionHandlerWhichFailsToStop();

        try
        {
            interceptor.stop();
            fail("was expecting a failure");
        }
        catch (Exception e)
        {
            assertThat(e, is(sameInstance(exception)));
        }

        verify(connectionHandler).stop();

        //stop again to verify that the write lock was released. If it didn't then this will hang
        //and the test will time out
        try
        {
            interceptor.stop();
            fail("re stopping should fail");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    @Test
    public void dispose()
    {
        interceptor.dispose();
        assertThat(connectionHandler.getDispose(), is(1));
    }

    private Exception setConnectionHandlerWhichFailsToDisconnect() throws Exception
    {
        connectionHandler = mock(PetStoreClientConnectionHandler.class);
        final Exception exception = new RuntimeException();
        doThrow(exception).when(connectionHandler).disconnect(Mockito.any(PetStoreClient.class));
        when(connectionHandler.connect(config)).thenReturn(mock(PetStoreClient.class));
        before();

        return exception;
    }

    private Exception setConnectionHandlerWhichFailsToStop() throws Exception
    {
        connectionHandler = mock(PetStoreClientConnectionHandler.class);
        final Exception exception = new RuntimeException();
        doThrow(exception).when(connectionHandler).stop();
        before();

        return exception;
    }


    private PetStoreClient getConnection() throws Exception
    {
        ArgumentCaptor<PetStoreClient> connectionCaptor = ArgumentCaptor.forClass(PetStoreClient.class);
        interceptor.before(operationContext);

        verify(operationContext, atLeastOnce()).setVariable(same(CONNECTION_PARAM), connectionCaptor.capture());
        return connectionCaptor.getValue();
    }
}
