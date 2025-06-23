/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.config;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_DEPLOYMENT_PROPERTY;

import static java.lang.Boolean.TRUE;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.junit.rules.ExpectedException.none;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.DeploymentInitException;
import org.mule.runtime.deployment.model.api.DeploymentStartException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.module.tooling.internal.ApplicationSupplier;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Properties;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class DefaultDeclarationSessionTestCase extends AbstractMuleTestCase {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  private static final String EXPECTED_EXCEPTION = "Expected exception";
  private static final String CONFIG_NAME = "configName";

  @Rule
  public ExpectedException expectedException = none();

  @Mock
  private ApplicationSupplier applicationSupplier;
  @Mock
  private Application application;

  @Before
  public void before() {
    final Properties deploymentProperties = new Properties();
    deploymentProperties.put(MULE_LAZY_INIT_DEPLOYMENT_PROPERTY, TRUE.toString());

    when(application.getDescriptor()).thenReturn(new ApplicationDescriptor("app", of(deploymentProperties)));
    final Registry registry = mock(Registry.class);
    when(registry.lookupByType(any(Class.class))).thenReturn(empty());
    final ArtifactContext artifactContext = mock(ArtifactContext.class);
    when(artifactContext.getRegistry()).thenReturn(registry);
    when(application.getArtifactContext()).thenReturn(artifactContext);
    when(application.getRegistry()).thenReturn(registry);
  }

  @Test
  public void deploymentException() throws Exception {
    when(applicationSupplier.get()).thenThrow(new DeploymentException(createStaticMessage(EXPECTED_EXCEPTION)));

    DefaultDeclarationSession defaultDeclarationSession = new DefaultDeclarationSession(applicationSupplier, null);

    expectedException.expect(DeploymentException.class);
    expectedException.expectMessage(EXPECTED_EXCEPTION);
    defaultDeclarationSession.testConnection(CONFIG_NAME);
  }

  @Test
  public void deploymentInitException() throws Exception {
    when(applicationSupplier.get()).thenReturn(application);
    doThrow(new DeploymentInitException(createStaticMessage(EXPECTED_EXCEPTION))).when(application).start();

    DefaultDeclarationSession defaultDeclarationSession = new DefaultDeclarationSession(applicationSupplier, null);

    expectedException.expect(DeploymentInitException.class);
    expectedException.expectMessage(EXPECTED_EXCEPTION);
    defaultDeclarationSession.testConnection(CONFIG_NAME);
  }


  @Test
  public void deploymentStartException() throws Exception {
    when(applicationSupplier.get()).thenReturn(application);
    doThrow(new DeploymentStartException(createStaticMessage(EXPECTED_EXCEPTION))).when(application).start();

    DefaultDeclarationSession defaultDeclarationSession = new DefaultDeclarationSession(applicationSupplier, null);

    expectedException.expect(DeploymentStartException.class);
    expectedException.expectMessage(EXPECTED_EXCEPTION);
    defaultDeclarationSession.testConnection(CONFIG_NAME);
  }
}
