/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.transport.jms;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.transport.jms.CustomCachingConnectionFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.lang.reflect.Proxy;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.junit.Test;


@SmallTest
public class CustomCachingConnectionFactoryTestCase extends AbstractMuleTestCase
{

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";

    private ConnectionFactory delegate = mock(ConnectionFactory.class);
    private Connection connection = mock(Connection.class);

    @Test
    public void createsConnection() throws Exception
    {
        when(delegate.createConnection()).thenReturn(connection);
        CustomCachingConnectionFactory cachingConnectionFactory = new CustomCachingConnectionFactory(delegate, null, null);

        Connection actualConnection = cachingConnectionFactory.createConnection();

        assertThat(actualConnection, is(instanceOf(Proxy.class)));
        verify(delegate.createConnection());
    }

    @Test
    public void createsConnectionWithUsername() throws Exception
    {
        when(delegate.createConnection(USERNAME, null)).thenReturn(connection);


        CustomCachingConnectionFactory cachingConnectionFactory = new CustomCachingConnectionFactory(delegate, USERNAME, null);

        Connection actualConnection = cachingConnectionFactory.createConnection();

        assertThat(actualConnection, is(instanceOf(Proxy.class)));
        verify(delegate.createConnection(USERNAME, null));
    }

    @Test
    public void createsConnectionWithPassword() throws Exception
    {
        when(delegate.createConnection(null, PASSWORD)).thenReturn(connection);


        CustomCachingConnectionFactory cachingConnectionFactory = new CustomCachingConnectionFactory(delegate, null, PASSWORD);

        Connection actualConnection = cachingConnectionFactory.createConnection();

        assertThat(actualConnection, is(instanceOf(Proxy.class)));
        verify(delegate.createConnection(null, PASSWORD));
    }

    @Test
    public void createsConnectionWithUsernameAndPassword() throws Exception
    {
        when(delegate.createConnection(USERNAME, PASSWORD)).thenReturn(connection);


        CustomCachingConnectionFactory cachingConnectionFactory = new CustomCachingConnectionFactory(delegate, USERNAME, PASSWORD);

        Connection actualConnection = cachingConnectionFactory.createConnection();

        assertThat(actualConnection, is(instanceOf(Proxy.class)));
        verify(delegate.createConnection(USERNAME, PASSWORD));
    }

    @Test(expected = javax.jms.IllegalStateException.class)
    public void throwsErrorCreatingConnectionWithCustomUsernameAndPassword() throws Exception
    {

        CustomCachingConnectionFactory cachingConnectionFactory = new CustomCachingConnectionFactory(delegate, USERNAME, PASSWORD);
        cachingConnectionFactory.createConnection(null, null);
    }

    @Test
    public void createsSingleConnection() throws Exception
    {
        when(delegate.createConnection(USERNAME, PASSWORD)).thenReturn(connection).thenReturn(null);


        CustomCachingConnectionFactory cachingConnectionFactory = new CustomCachingConnectionFactory(delegate, USERNAME, PASSWORD);
        Connection connection1 = cachingConnectionFactory.createConnection();
        connection1.close();

        Connection connection2 = cachingConnectionFactory.createConnection();
        assertThat(connection2, equalTo(connection1));
    }

    @Test
    public void cachesSession() throws Exception
    {
        when(delegate.createConnection(USERNAME, PASSWORD)).thenReturn(connection).thenReturn(null);
        Session session = mock(Session.class);
        when(connection.createSession(false, Session.AUTO_ACKNOWLEDGE)).thenReturn(session).thenReturn(null);

        CustomCachingConnectionFactory cachingConnectionFactory = new CustomCachingConnectionFactory(delegate, USERNAME, PASSWORD);
        Connection connection1 = cachingConnectionFactory.createConnection();
        Session session1 = connection1.createSession(false, Session.AUTO_ACKNOWLEDGE);

        session1.close();

        Session session2 = connection1.createSession(false, Session.AUTO_ACKNOWLEDGE);

        assertThat(session1, equalTo(session2));
    }

    @Test
    public void cachesProducer() throws Exception
    {
        Destination destination = mock(Destination.class);

        when(delegate.createConnection(USERNAME, PASSWORD)).thenReturn(connection).thenReturn(null);
        Session session = mock(Session.class);
        when(connection.createSession(false, Session.AUTO_ACKNOWLEDGE)).thenReturn(session).thenReturn(null);
        MessageProducer producer = mock(MessageProducer.class);
        when(session.createProducer(destination)).thenReturn(producer).thenReturn(null);

        CustomCachingConnectionFactory cachingConnectionFactory = new CustomCachingConnectionFactory(delegate, USERNAME, PASSWORD);

        Connection connection1 = cachingConnectionFactory.createConnection();
        Session session1 = connection1.createSession(false, Session.AUTO_ACKNOWLEDGE);
        final MessageProducer producer1 = session1.createProducer(destination);
        producer1.close();
        session1.close();

        Session session2 = connection1.createSession(false, Session.AUTO_ACKNOWLEDGE);
        final MessageProducer producer2 = session2.createProducer(destination);

        assertThat(producer1.toString(), equalTo(producer2.toString()));
    }
}
