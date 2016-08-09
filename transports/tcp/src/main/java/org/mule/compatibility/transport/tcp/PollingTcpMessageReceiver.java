/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.transport.AbstractPollingMessageReceiver;
import org.mule.compatibility.transport.tcp.i18n.TcpMessages;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.lifecycle.CreateException;
import org.mule.runtime.core.util.MapUtils;

import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * <code>PollingTcpMessageReceiver</code> acts like a TCP client polling for new messages.
 * 
 * @author esteban.robles
 */
public class PollingTcpMessageReceiver extends AbstractPollingMessageReceiver {

  private int timeout;

  private PollingTcpConnector connector;

  public PollingTcpMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint)
      throws CreateException {
    super(connector, flowConstruct, endpoint);

    if (connector instanceof PollingTcpConnector) {
      this.connector = (PollingTcpConnector) connector;
    } else {
      throw new CreateException(TcpMessages.pollingReceiverCannotbeUsed(), this);
    }

    timeout = MapUtils.getIntValue(endpoint.getProperties(), "clientSoTimeout", this.connector.getClientSoTimeout());

    if (timeout > Integer.MAX_VALUE || timeout < 0) {
      throw new IllegalArgumentException("Timeout incorrect: " + timeout);
    }

    long pollingFrequency =
        MapUtils.getLongValue(endpoint.getProperties(), "pollingFrequency", this.connector.getPollingFrequency());
    if (pollingFrequency > 0) {
      this.setFrequency(pollingFrequency);
    }
  }

  @Override
  public void poll() throws Exception {
    Socket socket = connector.getSocket(endpoint);
    try {
      Object result = TcpMessageDispatcher.receiveFromSocket(socket, timeout, endpoint);
      if (!(result == null)) {
        this.routeMessage(MuleMessage.builder().payload(result).build());
        if (logger.isDebugEnabled()) {
          logger.debug("Routing new message: " + result);
        }
      }
    } catch (SocketTimeoutException e) {
      if (logger.isDebugEnabled()) {
        logger.debug("Socket timed out normally while doing a synchronous receive on endpointUri: " + endpoint.getEndpointURI());
      }
    } finally {
      connector.releaseSocket(socket, endpoint);
    }
  }
}
