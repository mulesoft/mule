/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.config;

import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.DeploymentInitException;
import org.mule.runtime.deployment.model.api.DeploymentStartException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.tooling.internal.ApplicationSupplier;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefaultDeclarationSessionTestCase extends AbstractMuleTestCase {

  private static final String EXPECTED_EXCEPTION = "Expected exception";
  private static final String CONFIG_NAME = "configName";

  @Rule
  public ExpectedException expectedException = none();

  @Mock
  private ApplicationSupplier applicationSupplier;
  @Mock
  private Application application;

  @Test
  public void deploymentException() throws Exception {
    when(applicationSupplier.get()).thenThrow(new DeploymentException(createStaticMessage(EXPECTED_EXCEPTION)));

    DefaultDeclarationSession defaultDeclarationSession = new DefaultDeclarationSession(applicationSupplier);

    expectedException.expect(DeploymentException.class);
    expectedException.expectMessage(EXPECTED_EXCEPTION);
    defaultDeclarationSession.testConnection(CONFIG_NAME);
  }

  @Test
  public void deploymentInitException() throws Exception {
    when(applicationSupplier.get()).thenReturn(application);
    doThrow(new DeploymentInitException(createStaticMessage(EXPECTED_EXCEPTION))).when(application).start();

    DefaultDeclarationSession defaultDeclarationSession = new DefaultDeclarationSession(applicationSupplier);

    expectedException.expect(DeploymentInitException.class);
    expectedException.expectMessage(EXPECTED_EXCEPTION);
    defaultDeclarationSession.testConnection(CONFIG_NAME);
  }


  @Test
  public void deploymentStartException() throws Exception {
    when(applicationSupplier.get()).thenReturn(application);
    doThrow(new DeploymentStartException(createStaticMessage(EXPECTED_EXCEPTION))).when(application).start();

    DefaultDeclarationSession defaultDeclarationSession = new DefaultDeclarationSession(applicationSupplier);

    expectedException.expect(DeploymentStartException.class);
    expectedException.expectMessage(EXPECTED_EXCEPTION);
    defaultDeclarationSession.testConnection(CONFIG_NAME);
  }
}
