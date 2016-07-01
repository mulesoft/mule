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
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.sql.Connection;

import javax.sql.DataSource;

import org.junit.Test;

@SmallTest
public class SimpleConnectionFactoryTestCase extends AbstractMuleTestCase
{

    private final SimpleConnectionFactory connectionFactory = new SimpleConnectionFactory(null);
    private final DataSource dataSource = mock(DataSource.class);

    @Test
    public void createsConnection() throws Exception
    {
        Connection expectedConnection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(expectedConnection);

        Connection connection = connectionFactory.create(dataSource);
        assertThat(connection, equalTo(expectedConnection));
    }

    @Test(expected = ConnectionCreationException.class)
    public void failsOnConnectionError() throws Exception
    {
        when(dataSource.getConnection()).thenThrow(new RuntimeException());

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