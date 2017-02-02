/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http;

import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;

import org.mule.compatibility.core.api.transport.NoReceiverForEndpointException;
import org.mule.compatibility.transport.http.i18n.HttpMessages;
import org.mule.runtime.core.util.monitor.Expirable;

import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dispatches HttpRequest to the appropriate MessageReceiver
 */
public class HttpRequestDispatcherWork implements Runnable, Expirable {

  private static Logger logger = LoggerFactory.getLogger(HttpRequestDispatcherWork.class);

  private HttpServerConnection httpServerConnection;
  private Socket socket;
  private HttpConnector httpConnector;

  public HttpRequestDispatcherWork(HttpConnector httpConnector, Socket socket) {
    if (httpConnector == null) {
      throw new IllegalArgumentException("HttpConnector can not be null");
    }
    if (socket == null) {
      throw new IllegalArgumentException("Socket can not be null");
    }
    this.httpConnector = httpConnector;
    this.socket = socket;
  }

  @Override
  public void run() {
    try {
      long keepAliveTimeout = httpConnector.getKeepAliveTimeout();
      Charset encoding = getDefaultEncoding(httpConnector.getMuleContext());
      httpServerConnection = new HttpServerConnection(socket, encoding, httpConnector);
      do {
        try {

          httpServerConnection.setKeepAlive(false);

          // Only add a monitor if the timeout has been set
          if (keepAliveTimeout > 0) {
            httpConnector.getKeepAliveMonitor().addExpirable(keepAliveTimeout, TimeUnit.MILLISECONDS, this);
          }

          RequestLine requestLine = httpServerConnection.getRequestLine();
          if (requestLine != null) {
            try {
              HttpMessageReceiver httpMessageReceiver = httpConnector.lookupReceiver(socket, requestLine);
              httpMessageReceiver.processRequest(httpServerConnection);
            } catch (NoReceiverForEndpointException e) {
              httpServerConnection
                  .writeFailureResponse(HttpConstants.SC_NOT_FOUND,
                                        HttpMessages.cannotBindToAddress(httpServerConnection.getFullUri()).toString());
            }
          }
        } finally {
          httpConnector.getKeepAliveMonitor().removeExpirable(this);
          httpServerConnection.reset();
        }
      } while (httpServerConnection.isKeepAlive());
    } catch (HttpMessageReceiver.EmptyRequestException e) {
      logger.debug("Discarding request since content was empty");
    } catch (HttpMessageReceiver.FailureProcessingRequestException e) {
      logger.debug("Closing socket due to failure during request processing");
    } catch (Exception e) {
      httpConnector.getMuleContext().getExceptionListener().handleException(e);
    } finally {
      logger.debug("Closing HTTP connection.");
      if (httpServerConnection != null && httpServerConnection.isOpen()) {
        httpServerConnection.close();
        httpServerConnection = null;
      }
    }
  }

  @Override
  public void expired() {
    if (httpServerConnection.isOpen()) {
      httpServerConnection.close();
    }
  }

}
