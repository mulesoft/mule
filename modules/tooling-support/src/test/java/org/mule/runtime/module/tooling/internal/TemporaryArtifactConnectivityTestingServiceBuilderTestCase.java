/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal;

import static java.util.Arrays.asList;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.dsl.api.config.ArtifactConfiguration;
import org.mule.runtime.dsl.api.config.ComponentConfiguration;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.module.repository.api.RepositoryService;
import org.mule.runtime.module.deployment.impl.internal.artifact.TemporaryArtifactBuilderFactory;
import org.mule.runtime.core.api.connectivity.ConnectivityTestingStrategy;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TemporaryArtifactConnectivityTestingServiceBuilderTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = none();

  private RepositoryService mockRepositoryService = mock(RepositoryService.class, RETURNS_DEEP_STUBS);
  private TemporaryArtifactBuilderFactory mockArtifactBuilderFactory =
      mock(TemporaryArtifactBuilderFactory.class, RETURNS_DEEP_STUBS);
  private ServiceRegistry mockServiceRegistry = mock(ServiceRegistry.class);
  private DefaultConnectivityTestingServiceBuilder builder;

  @Before
  public void createConnectivityTestingBuilder() {
    builder =
        new DefaultConnectivityTestingServiceBuilder(mockRepositoryService, mockArtifactBuilderFactory, mockServiceRegistry);
  }

  @Test(expected = IllegalStateException.class)
  public void noExtensionConfigured() {
    builder.setArtifactConfiguration(getArtifactConfiguration()).build();
  }

  @Test(expected = IllegalStateException.class)
  public void noArtifactConfiguration() {
    addExtension().build();
  }

  @Test
  public void buildArtifact() {
    when(mockServiceRegistry.lookupProviders(eq(ConnectivityTestingStrategy.class), any()))
        .thenReturn(asList(mock(ConnectivityTestingStrategy.class)));
    addExtension().setArtifactConfiguration(getArtifactConfiguration());

    builder.build();
  }

  private DefaultConnectivityTestingServiceBuilder addExtension() {
    builder.addExtension("groupId", "artifactId", "version");
    return builder;
  }

  private ArtifactConfiguration getArtifactConfiguration() {
    return new ArtifactConfiguration(asList(new ComponentConfiguration.Builder().setIdentifier("identifier")
        .setNamespace("namespace").build()));
  }

}
