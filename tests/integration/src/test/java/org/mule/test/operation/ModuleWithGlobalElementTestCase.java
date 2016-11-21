/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.operation;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.mule.extension.http.api.request.validator.ResponseValidatorException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.IOException;
import java.util.Base64;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ModuleWithGlobalElementTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort httpPort = new DynamicPort("httpPort");

  private Server server;

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

  @Override
  protected String getConfigFile() {
    return "module/flows-using-module-global-elements.xml";
  }

  //@Override
  //protected Class<?>[] getAnnotatedExtensionClasses() {
  //  //TODO until MULE-10383 is fixed both Socket and Http extensions will be exposed, instead of just the Http one
  //  return new Class[] {SocketsExtension.class, HttpConnector.class};
  //}
  //
  ///**
  // * The test cannot run with isolation due to http ext doesn't have anymore the mule-module.properties. This test needs to have
  // * the complete access to all the classes and resources therefore it just returns the class loader that loaded the test class.
  // * (taken from AbstractTlsRestrictedProtocolsAndCiphersTestCase test)
  // *
  // * @return the {@link ClassLoader} that loaded the test.
  // */
  //@Override
  //protected ClassLoader getExecutionClassLoader() {
  //  return this.getClass().getClassLoader();
  //}

  @Test
  public void testHttpDoLogin() throws Exception {
    Event muleEvent = flowRunner("testHttpDoLogin").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), Is.is("success with basic-authentication for user: userLP"));
  }

  @Test
  public void testHttpDontLogin() throws Exception {
    try {
      flowRunner("testHttpDontLogin").run();
      fail("Should not have reach here");
    } catch (MessagingException me) {
      assertThat(me.getCause(), instanceOf(ResponseValidatorException.class));
      assertThat(me.getCause().getMessage(), Is.is("Response code 401 mapped as failure"));
    }
  }

  @Test
  public void testHttpDoLoginGonnet() throws Exception {
    Event muleEvent = flowRunner("testHttpDoLoginGonnet").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), Is.is("success with basic-authentication for user: userGonnet"));
  }

  /**
   * Really simple handler for basic authentication where the user and pass, once decoded, must match the path of the request.
   * For example: "/basic-aith/userLP/passLP" request must have an "Authorization" header with "userLP:passLP" encoded in Base64
   * to return 200, otherwise it will be 401 (unauthorized)
   */
  private class SimpleBasicAuthentication extends AbstractHandler {

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
      int scUnauthorized;
      String message;

      String userAndPass = new String(Base64.getDecoder().decode(request.getHeader("Authorization").substring("Basic ".length())))
          .replace(':', '/');
      if (target.endsWith(userAndPass)) {
        scUnauthorized = HttpServletResponse.SC_OK;
        message = "User and pass validated";
      } else {
        scUnauthorized = HttpServletResponse.SC_UNAUTHORIZED;
        message = "User and pass wrong";
      }
      response.setStatus(scUnauthorized);
      response.getWriter().print(message);
      response.setContentType("text/html");
      baseRequest.setHandled(true);
    }
  }
}
