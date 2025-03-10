/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config.bootstrap;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.DOMAIN;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.createFromString;
import static org.mule.runtime.core.internal.config.bootstrap.AbstractRegistryBootstrap.APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY;
import static org.mule.runtime.core.internal.config.bootstrap.AbstractRegistryBootstrap.BINDING_PROVIDER_PREDICATE;

import static java.lang.String.format;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.config.bootstrap.BootstrapServiceDiscoverer;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import org.junit.Test;

import io.qameta.allure.Issue;

public class SimpleRegistryBootstrapTestCase extends AbstractMuleContextTestCase {

  @Test
  public void registerOnlyAppPropertiesType() throws Exception {
    createTestRegistryBootstrap(APP);
    final MuleRegistry registry = ((MuleContextWithRegistry) muleContext).getRegistry();
    assertThat(registry.lookupObject(String.class), notNullValue());
    assertThat(registry.lookupObject(Properties.class), nullValue());
    assertThat(registry.lookupObject(HashMap.class), nullValue());
    assertThat(registry.lookupObject(ArrayList.class), notNullValue());
  }

  @Test
  public void registerOnlyDomainPropertiesType() throws Exception {
    createTestRegistryBootstrap(DOMAIN);
    final MuleRegistry registry = ((MuleContextWithRegistry) muleContext).getRegistry();
    assertThat(registry.lookupObject(String.class), nullValue());
    assertThat(registry.lookupObject(Properties.class), notNullValue());
    assertThat(registry.lookupObject(HashMap.class), nullValue());
    assertThat(registry.lookupObject(ArrayList.class), notNullValue());
  }

  @Test
  public void registerOnlyPolicyPropertiesType() throws Exception {
    createTestRegistryBootstrap(POLICY);
    final MuleRegistry registry = ((MuleContextWithRegistry) muleContext).getRegistry();
    assertThat(registry.lookupObject(String.class), nullValue());
    assertThat(registry.lookupObject(Properties.class), nullValue());
    assertThat(registry.lookupObject(HashMap.class), notNullValue());
    assertThat(registry.lookupObject(ArrayList.class), notNullValue());
  }

  private SimpleRegistryBootstrap createTestRegistryBootstrap(ArtifactType artifactType) throws InitialisationException {
    final Properties properties = new Properties();
    properties.put("1", format("java.lang.String,%s=%s",
                               APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY,
                               APP.getAsString()));
    properties.put("2", format("java.util.Properties,%s=%s",
                               APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY,
                               DOMAIN.getAsString()));
    properties.put("3", format("java.util.HashMap,%s=%s",
                               APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY,
                               POLICY.getAsString()));
    properties.put("4", format("java.util.ArrayList,%s=%s/%s/%s",
                               APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY,
                               APP.getAsString(),
                               DOMAIN.getAsString(),
                               POLICY.getAsString()));

    final BootstrapServiceDiscoverer bootstrapServiceDiscoverer = new TestBootstrapServiceDiscoverer(properties);
    muleContext.setBootstrapServiceDiscoverer(bootstrapServiceDiscoverer);

    SimpleRegistryBootstrap simpleRegistryBootstrap =
        new SimpleRegistryBootstrap(artifactType, muleContext.getRegistryBootstrapServiceDiscoverer(),
                                    ((MuleContextWithRegistry) muleContext).getRegistry()::registerObject);
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
  public void createArtifactFromString() {
    assertThat(createFromString("domain"), is(DOMAIN));
  }

  @Test(expected = MuleRuntimeException.class)
  public void createArtifactFromStringWithError() {
    createFromString("Messi");
  }

  @Test
  public void toNewVersion() {
    assertThat(DOMAIN.getArtifactType().getArtifactTypeAsString(), is("domain"));
  }
}
