/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config.bootstrap;

import static org.mule.runtime.api.artifact.ArtifactType.APP;
import static org.mule.runtime.api.artifact.ArtifactType.DOMAIN;
import static org.mule.runtime.api.artifact.ArtifactType.POLICY;
import static org.mule.runtime.core.internal.config.bootstrap.AbstractRegistryBootstrap.APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY;
import static org.mule.runtime.core.internal.config.bootstrap.AbstractRegistryBootstrap.BINDING_PROVIDER_PREDICATE;

import static java.lang.String.format;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

import org.mule.runtime.api.artifact.ArtifactType;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.config.bootstrap.BootstrapServiceDiscoverer;
import org.mule.runtime.core.internal.registry.Registry;
import org.mule.runtime.core.internal.registry.SimpleRegistry;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import org.junit.Test;

import io.qameta.allure.Issue;

public class SimpleRegistryBootstrapTestCase extends AbstractMuleTestCase {

  @Test
  public void registerOnlyAppPropertiesType() throws Exception {
    final Registry registry = new SimpleRegistry(null, null);
    createTestRegistryBootstrap(APP, registry);
    assertThat(registry.lookupObject(String.class), notNullValue());
    assertThat(registry.lookupObject(Properties.class), nullValue());
    assertThat(registry.lookupObject(HashMap.class), nullValue());
    assertThat(registry.lookupObject(ArrayList.class), notNullValue());
  }

  @Test
  public void registerOnlyDomainPropertiesType() throws Exception {
    final Registry registry = new SimpleRegistry(null, null);
    createTestRegistryBootstrap(DOMAIN, registry);
    assertThat(registry.lookupObject(String.class), nullValue());
    assertThat(registry.lookupObject(Properties.class), notNullValue());
    assertThat(registry.lookupObject(HashMap.class), nullValue());
    assertThat(registry.lookupObject(ArrayList.class), notNullValue());
  }

  @Test
  public void registerOnlyPolicyPropertiesType() throws Exception {
    final Registry registry = new SimpleRegistry(null, null);
    createTestRegistryBootstrap(POLICY, registry);
    assertThat(registry.lookupObject(String.class), nullValue());
    assertThat(registry.lookupObject(Properties.class), nullValue());
    assertThat(registry.lookupObject(HashMap.class), notNullValue());
    assertThat(registry.lookupObject(ArrayList.class), notNullValue());
  }

  private SimpleRegistryBootstrap createTestRegistryBootstrap(ArtifactType artifactType, Registry registry)
      throws InitialisationException {
    final Properties properties = new Properties();
    properties.put("1", format("java.lang.String,%s=%s",
                               APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY,
                               APP.getArtifactTypeAsString()));
    properties.put("2", format("java.util.Properties,%s=%s",
                               APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY,
                               DOMAIN.getArtifactTypeAsString()));
    properties.put("3", format("java.util.HashMap,%s=%s",
                               APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY,
                               POLICY.getArtifactTypeAsString()));
    properties.put("4", format("java.util.ArrayList,%s=%s/%s/%s",
                               APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY,
                               APP.getArtifactTypeAsString(),
                               DOMAIN.getArtifactTypeAsString(),
                               POLICY.getArtifactTypeAsString()));

    final BootstrapServiceDiscoverer bootstrapServiceDiscoverer = new TestBootstrapServiceDiscoverer(properties);
    SimpleRegistryBootstrap simpleRegistryBootstrap = new SimpleRegistryBootstrap(artifactType, bootstrapServiceDiscoverer,
                                                                                  registry::registerObject);
    simpleRegistryBootstrap.initialise();
    return simpleRegistryBootstrap;
  }

  @Test
  @Issue("MULE-20041")
  public void bindingProviderPredicate() {
    assertThat(BINDING_PROVIDER_PREDICATE.test("someFunctionsProvider"), is(true));
    assertThat(BINDING_PROVIDER_PREDICATE.test("my.bindings.provider"), is(false));
    assertThat(BINDING_PROVIDER_PREDICATE.test("someRandomEntry"), is(false));
  }

  @Test
  public void toNewVersion() {
    assertThat(DOMAIN.getArtifactTypeAsString(), is("domain"));
  }
}
