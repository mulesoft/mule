/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.http.impl.service.server.grizzly;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mule.service.http.api.HttpConstants.HttpStatus.OK;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.api.server.HttpServer;
import org.mule.service.http.api.server.async.ResponseStatusCallback;
import org.mule.service.http.api.tcp.TcpServerSocketProperties;
import org.mule.services.http.impl.service.server.DefaultServerAddress;
import org.mule.services.http.impl.service.server.HttpListenerRegistry;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class GrizzlyServerManagerTestCase extends AbstractMuleContextTestCase {

  @Rule
  public DynamicPort listenerPort = new DynamicPort("listener.port");

  private ExecutorService selectorPool;
  private ExecutorService workerPool;
  private ExecutorService idleTimeoutExecutorService;

  @Before
  public void before() {
    selectorPool = newCachedThreadPool();
    workerPool = newCachedThreadPool();
    idleTimeoutExecutorService = newCachedThreadPool();
  }

  @After
  public void after() {
    selectorPool.shutdown();
    workerPool.shutdown();
    idleTimeoutExecutorService.shutdown();
  }

  @Test
  public void managerDisposeClosesServerOpenConnections() throws IOException {
    final GrizzlyServerManager serverManager =
        new GrizzlyServerManager(selectorPool, workerPool, idleTimeoutExecutorService, new HttpListenerRegistry(),
                                 new DefaultTcpServerSocketProperties());

    final HttpServer server = serverManager.createServerFor(new DefaultServerAddress("0.0.0.0", listenerPort.getNumber()),
                                                            () -> muleContext.getSchedulerService().ioScheduler(), true,
                                                            (int) SECONDS.toMillis(DEFAULT_TEST_TIMEOUT_SECS));
    final ResponseStatusCallback responseStatusCallback = mock(ResponseStatusCallback.class);
    server.addRequestHandler("/path", (requestContext, responseCallback) -> {
      responseCallback.responseReady(HttpResponse.builder().setStatusCode(OK.getStatusCode()).build(),
                                     responseStatusCallback);
    });
    server.start();

    try (Socket clientSocket = new Socket("localhost", listenerPort.getNumber())) {
      final PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
      writer.println("GET /path HTTP/1.1");
      writer.println("Host: localhost");
      writer.println("");
      writer.flush();

      BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      String t;
      while ((t = br.readLine()) != null) {
        if (t.equals("0")) {
          break;
        }
      }

      verify(responseStatusCallback, timeout(1000)).responseSendSuccessfully();
      server.stop();
      serverManager.dispose();

      while ((t = br.readLine()) != null) {
        // Empty the buffer
      }
      br.close();
    }
  }

  private class DefaultTcpServerSocketProperties implements TcpServerSocketProperties {

    @Override
    public Integer getSendBufferSize() {
      return null;
    }

    @Override
    public Integer getReceiveBufferSize() {
      return null;
    }

    @Override
    public Integer getClientTimeout() {
      return null;
    }

    @Override
    public Boolean getSendTcpNoDelay() {
      return true;
    }

    @Override
    public Integer getLinger() {
      return null;
    }

    @Override
    public Boolean getKeepAlive() {
      return false;
    }

    @Override
    public Boolean getReuseAddress() {
      return true;
    }

    @Override
    public Integer getReceiveBacklog() {
      return 50;
    }

    @Override
    public Integer getServerTimeout() {
      return null;
    }
  }

}
