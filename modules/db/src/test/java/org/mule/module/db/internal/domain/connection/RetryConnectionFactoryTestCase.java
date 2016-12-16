/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.connection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.Before;
import org.mule.api.context.WorkManager;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.retry.policies.AbstractPolicyTemplate;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.sql.Connection;

import javax.sql.DataSource;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@SmallTest
public class RetryConnectionFactoryTestCase extends AbstractMuleTestCase
{

    private final RetryPolicyTemplate retryPolicyTemplate = mock(RetryPolicyTemplate.class);
    private final ConnectionFactory delegate = mock(ConnectionFactory.class);
    private final DataSource dataSource = mock(DataSource.class);
    private RetryConnectionFactory connectionFactory;

    @Before
    public void init()
    {
        when(retryPolicyTemplate.isSynchronous()).thenReturn(true);
        connectionFactory = new RetryConnectionFactory(retryPolicyTemplate, delegate);
    }

    @Test
    public void createsConnection() throws Exception
    {
        final ArgumentCaptor<RetryCallback> retryCallbackArgumentCaptor = ArgumentCaptor.forClass(RetryCallback.class);

        when(retryPolicyTemplate.execute(retryCallbackArgumentCaptor.capture(), Matchers.<WorkManager>any())).then(new Answer<Object>()
        {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                RetryCallback retryCallback = retryCallbackArgumentCaptor.getValue();
                retryCallback.doWork(null);
                return null;
            }
        });

        Connection expectedConnection = mock(Connection.class);
        when(delegate.create(dataSource)).thenReturn(expectedConnection);

        Connection connection = connectionFactory.create(dataSource);
        assertThat(connection, equalTo(expectedConnection));
    }

    @Test(expected = ConnectionCreationException.class)
    public void failsOnConnectionError() throws Exception
    {
        final ArgumentCaptor<RetryCallback> retryCallbackArgumentCaptor = ArgumentCaptor.forClass(RetryCallback.class);

        when(retryPolicyTemplate.execute(retryCallbackArgumentCaptor.capture(), Matchers.<WorkManager>any())).then(new Answer<Object>()
        {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                throw new RuntimeException();
            }
        });

        connectionFactory.create(dataSource);
    }

    @Test(expected = ConnectionCreationException.class)
    public void failsOnNullConnection() throws Exception
    {
        Connection expectedConnection = null;
        when(dataSource.getConnection()).thenReturn(expectedConnection);

        connectionFactory.create(dataSource);
    }
}