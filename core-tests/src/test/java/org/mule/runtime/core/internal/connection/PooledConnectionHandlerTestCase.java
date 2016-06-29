/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mule.runtime.api.connection.PoolingListener;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.apache.commons.pool.ObjectPool;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class PooledConnectionHandlerTestCase extends AbstractMuleTestCase
{

    @Mock
    private ObjectPool<Object> pool;

    @Mock
    private Object config;

    @Mock
    private Object connection;

    @Mock
    private PoolingListener<Object> poolingListener;

    private PooledConnectionHandler<Object> managedConnection;

    @Before
    public void before()
    {
        managedConnection = new PooledConnectionHandler<>(connection, pool, poolingListener);
    }

    @Test
    public void getConnection() throws Exception
    {
        assertThat(managedConnection.getConnection(), is(sameInstance(connection)));
    }

    @Test
    public void release() throws Exception
    {
        managedConnection.release();
        verify(pool).returnObject(connection);
        verify(poolingListener).onReturn(connection);
    }

    @Test
    public void close() throws Exception
    {
        managedConnection.close();
        verify(pool, never()).returnObject(anyObject());
    }
}
