/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.ws.functional;


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.construct.Flow;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class BasicAuthWsdlFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "basic-auth-wsdl-config.xml";
  }

  @Parameterized.Parameters
  public static Collection<Object[]> parameters() {
    // Change default behavior of AbstractWSConsumerFunctionalTestCase as this test only uses the new connector.
    return Arrays.asList(new Object[][] {new Object[] {false}});
  }

  @Rule
  public DynamicPort httpServerPort = new DynamicPort("httpServerPort");

  private Server httpServer;

  /**
   * Since the fetching of the WSDL occurs during init, we cannot tie the server of the WSDL the the startup pf the context.
   * 
   * @throws Exception
   */
  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    httpServer = new Server(httpServerPort.getNumber());

    HashLoginService loginService = new HashLoginService();
    loginService.putUser("user", new Password("password"), new String[] {"user"});

    ConstraintSecurityHandler security = new ConstraintSecurityHandler();
    httpServer.setHandler(security);

    Constraint constraint = new Constraint();
    constraint.setName("auth");
    constraint.setAuthenticate(true);
    constraint.setRoles(new String[] {"user"});

    ConstraintMapping mapping = new ConstraintMapping();
    mapping.setPathSpec("/*");
    mapping.setConstraint(constraint);

    security.setConstraintMappings(Collections.singletonList(mapping));
    security.setAuthenticator(new BasicAuthenticator());
    security.setLoginService(loginService);

    ResourceHandler resourceHandler = new ResourceHandler();
    resourceHandler.setBaseResource(Resource.newClassPathResource("/"));

    HandlerList handlers = new HandlerList();
    handlers.setHandlers(new Handler[] {resourceHandler, new DefaultHandler()});
    security.setHandler(handlers);

    httpServer.start();
  }

  @Override
  protected void doTearDownAfterMuleContextDispose() throws Exception {
    httpServer.stop();
    httpServer.join();
  }

  @Test
  public void correctStartUp() throws Exception {
    Flow flow = (Flow) getFlowConstruct("authWsdl");
    assertThat(flow.isStarted(), is(true));
    // If we get this far, it means that the context has started, which is what we need to test.
  }
}
