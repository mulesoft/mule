/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;


import static java.util.Base64.getDecoder;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.tck.junit4.rule.DynamicPort;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public abstract class AbstractModuleWithHttpTestCase extends AbstractXmlExtensionMuleArtifactFunctionalTestCase {

  static final String MODULE_GLOBAL_ELEMENT_XML = "modules/module-global-element.xml";
  static final String MODULE_GLOBAL_ELEMENT_PROXY_XML = "modules/nested/module-global-element-proxy.xml";
  static final String MODULE_GLOBAL_ELEMENT_ANOTHER_PROXY_XML = "modules/nested/module-global-element-another-proxy.xml";

  private Server server;

  @Rule
  public DynamicPort httpPort = new DynamicPort("httpPort");

  @Before
  public void startServer() throws Exception {
    server = new Server(httpPort.getNumber());
    server.setHandler(new SimpleBasicAuthentication());
    server.start();
  }

  @After
  public void stopServer() throws Exception {
    if (server != null) {
      server.stop();
    }
  }

  /**
   * Asserts that a given flow can successfully be executed and it also checks that the authorization against the {@link SimpleBasicAuthentication}
   * handler does return a success response for the parametrized username
   * @param flowName to execute
   * @param username to validate after hitting the HTTP endpoint
   */
  protected void assertFlowForUsername(String flowName, String username) throws Exception {
    InternalEvent muleEvent = flowRunner(flowName).run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is("success with basic-authentication for user: " + username));
  }

  /**
   * Really simple handler for basic authentication where the user and pass, once decoded, must match the path of the request.
   * For example: "/basic-auth/userLP/passLP" request must have an "Authorization" header with "userLP:passLP" encoded in Base64
   * to return 200, otherwise it will be 401 (unauthorized)
   */
  private class SimpleBasicAuthentication extends AbstractHandler {

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
      int scUnauthorized;
      String message;
      String userAndPass = new String(getDecoder().decode(request.getHeader("Authorization").substring("Basic ".length())))
          .replace(':', '/');
      if (target.endsWith(userAndPass)) {
        scUnauthorized = SC_OK;
        message = "User and pass validated";
      } else {
        scUnauthorized = SC_UNAUTHORIZED;
        message = "User and pass wrong";
      }
      response.setStatus(scUnauthorized);
      response.getWriter().print(message);
      response.setContentType("text/html");
      baseRequest.setHandled(true);
    }
  }

}
