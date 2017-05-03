/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.runtime.transport;

import static org.eclipse.jetty.servlet.ServletContextHandler.SESSIONS;
import static org.eclipse.jetty.util.security.Constraint.__BASIC_AUTH;
import static org.eclipse.jetty.util.security.Credential.getCredential;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mule.extension.ws.WscTestUtils.ECHO;
import static org.mule.extension.ws.WscTestUtils.assertSoapResponse;
import static org.mule.extension.ws.WscTestUtils.getRequestResource;
import static org.mule.services.soap.api.exception.error.SoapErrors.CANNOT_DISPATCH;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import static org.mule.test.allure.AllureConstants.WscFeature.WSC_EXTENSION;
import org.mule.extension.ws.AbstractSoapServiceTestCase;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.exception.MessagingException;

import java.util.Optional;

import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(WSC_EXTENSION)
@Stories({"Operation Execution", "Custom Transport", "Http"})
public class HttpBasicAuthConfigTestCase extends AbstractSoapServiceTestCase {

  @Test
  public void httpBasicAuthConfiguration() throws Exception {
    Message message = runFlowWithRequest("basicAuthRequester", ECHO);
    String out = (String) message.getPayload().getValue();
    assertSoapResponse(ECHO, out);
  }

  @Test
  public void unauthorizedConfiguration() throws Exception {
    MessagingException exc = flowRunner("unauthorizedRequester").withPayload(getRequestResource(ECHO)).runExpectingException();
    Optional<Error> error = exc.getEvent().getError();
    assertThat(error.isPresent(), is(true));
    assertThat(error.get().getErrorType(), errorType("WSC", CANNOT_DISPATCH.toString()));
    assertThat(error.get().getDescription(), containsString("Response code 401 mapped as failure"));
  }

  @Override
  protected void createWebService() throws Exception {
    try {
      httpServer = new Server(servicePort.getNumber());
      CXFNonSpringServlet cxf = new CXFNonSpringServlet();
      ServletHolder servlet = new ServletHolder(cxf);
      servlet.setName("server");
      servlet.setForcedPath("/");
      ServletContextHandler context = new ServletContextHandler(SESSIONS);
      context.setSecurityHandler(getBasicAuth());
      context.setContextPath("/");
      context.addServlet(servlet, "/*");
      httpServer.setHandler(context);
      httpServer.start();
      initService(cxf);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // User = juani - Password = changeIt - Realm = private
  private SecurityHandler getBasicAuth() {
    HashLoginService l = new HashLoginService();
    l.putUser("juani", getCredential("changeIt"), new String[] {"user"});
    l.setName("private");

    Constraint constraint = new Constraint();
    constraint.setName(__BASIC_AUTH);
    constraint.setRoles(new String[]{"user"});
    constraint.setAuthenticate(true);

    ConstraintMapping cm = new ConstraintMapping();
    cm.setConstraint(constraint);
    cm.setPathSpec("/*");

    ConstraintSecurityHandler csh = new ConstraintSecurityHandler();
    csh.setAuthenticator(new BasicAuthenticator());
    csh.setRealmName("testRealm");
    csh.addConstraintMapping(cm);
    csh.setLoginService(l);
    return csh;
  }

  @Override
  protected String getConfigurationFile() {
    return "config/transport/basic-auth-http-custom-transport.xml";
  }
}
