/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4.rules;

import static org.mule.functional.util.http.SimpleHttpServer.createServer;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;

import org.mule.functional.util.http.SimpleHttpServer;
import org.mule.tck.junit4.rule.FreePortFinder;

import org.junit.rules.ExternalResource;

/**
 * JUnit rule to create an HTTP server
 *
 * @since 4.0
 *
 * @deprecated since 4.10 use WireMock instead
 */
@Deprecated
public class HttpServerRule extends ExternalResource {

  private final String portSystemPropertyKey;
  private SimpleHttpServer simpleHttpServer;

  /**
   * @param portSystemPropertyKey name of the system property where the server port will be placed.
   */
  public HttpServerRule(String portSystemPropertyKey) {
    this.portSystemPropertyKey = portSystemPropertyKey;
  }

  @Override
  protected void before() throws Throwable {
    Integer port = new FreePortFinder(7000, 9999).find();
    simpleHttpServer = createServer(port).start();
    setProperty(portSystemPropertyKey, String.valueOf(port));
  }

  @Override
  protected void after() {
    clearProperty(portSystemPropertyKey);
    simpleHttpServer.stop();
  }

  /**
   * @return the http server created by the rule.
   */
  public SimpleHttpServer getSimpleHttpServer() {
    return simpleHttpServer;
  }
}
