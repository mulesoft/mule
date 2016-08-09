/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket;

import static java.lang.String.format;
import static org.mockito.Mockito.when;
import static org.mule.extension.socket.SocketExtensionTestCase.POLL_DELAY_MILLIS;
import static org.mule.extension.socket.SocketExtensionTestCase.TIMEOUT_MILLIS;
import org.mule.extension.socket.api.ConnectionSettings;
import org.mule.extension.socket.api.connection.tcp.TcpListenerConnection;
import org.mule.extension.socket.api.connection.tcp.TcpRequesterConnection;
import org.mule.extension.socket.api.connection.tcp.protocol.SafeProtocol;
import org.mule.extension.socket.api.socket.factory.TcpServerSocketFactory;
import org.mule.extension.socket.api.socket.factory.TcpSocketFactory;
import org.mule.extension.socket.api.socket.tcp.TcpClientSocketProperties;
import org.mule.extension.socket.api.socket.tcp.TcpServerSocketProperties;
import org.mule.extension.socket.api.worker.SocketWorker;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.runtime.MessageHandler;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.size.SmallTest;

import java.net.InetSocketAddress;
import java.net.SocketException;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class TcpReconnectionTestCase extends AbstractMuleTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Inject
  private MuleContext muleContext;

  @Mock
  private ConnectionSettings connectionSettings;

  @Mock
  private MessageHandler handler;

  private TcpServerSocketProperties serverSocketProperties;
  private TcpClientSocketProperties clientSocketProperties;

  private TcpListenerConnection listenerConnection;
  private TcpRequesterConnection requesterConnection;

  private String host;
  private int port;

  @Before
  public void before() throws ConnectionException

  {
    port = dynamicPort.getNumber();
    host = "localhost";
    when(connectionSettings.getPort()).thenReturn(port);
    when(connectionSettings.getHost()).thenReturn(host);
    when(connectionSettings.getInetSocketAddress()).thenReturn(new InetSocketAddress(host, port));

    // Mockito returns 0 in mocked boxed types, that leads to invalid socket configurations
    serverSocketProperties = new TcpServerSocketProperties();
    clientSocketProperties = new TcpClientSocketProperties();

    listenerConnection =
        new TcpListenerConnection(connectionSettings, new SafeProtocol(), serverSocketProperties, new TcpServerSocketFactory());
    requesterConnection = new TcpRequesterConnection(connectionSettings, new ConnectionSettings(), new SafeProtocol(),
                                                     clientSocketProperties, new TcpSocketFactory());
  }

  @Test
  public void failOnInvalidPort() throws Exception {
    int invalidPort = -1;
    connectionSettings = new ConnectionSettings(invalidPort, host);
    listenerConnection =
        new TcpListenerConnection(connectionSettings, new SafeProtocol(), serverSocketProperties, new TcpServerSocketFactory());
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format("port out of range:%d", invalidPort));
    listenerConnection.connect();
  }

  @Test
  public void failToBindListenerInAnOccupiedPort() throws Exception {
    expectedException.expect(ConnectionException.class);
    expectedException.expectMessage(format("Could not bind socket to host '%s' and port '%d'", host, port));
    listenerConnection.connect();

    TcpListenerConnection secondListener =
        new TcpListenerConnection(connectionSettings, new SafeProtocol(), serverSocketProperties, new TcpServerSocketFactory());
    secondListener.connect();
  }

  @Test
  public void requesterFailsToConnect() throws Exception {
    expectedException.expect(ConnectionException.class);
    expectedException.expectMessage(format("Could not connect TCP requester socket to host '%s' on port '%d'", host, port));
    requesterConnection.connect();
  }

  @Test
  public void closeListener() throws Exception {
    expectedException.expect(SocketException.class);
    expectedException.expectMessage("Socket is closed");
    listenerConnection.connect();

    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
    prober.check(new JUnitLambdaProbe(() -> {
      if (listenerConnection.validate().isValid()) {
        listenerConnection.disconnect();
        return true;
      }
      return false;
    }));

    SocketWorker worker = listenerConnection.listen(handler);
  }
}
