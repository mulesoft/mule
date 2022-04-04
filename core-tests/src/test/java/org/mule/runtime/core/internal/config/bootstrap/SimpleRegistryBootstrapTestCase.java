/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config.bootstrap;

import static java.lang.String.format;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.DOMAIN;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;
import static org.mule.runtime.core.internal.config.bootstrap.AbstractRegistryBootstrap.BINDING_PROVIDER_PREDICATE;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.config.bootstrap.BootstrapServiceDiscoverer;
import org.mule.runtime.core.api.transaction.TransactionFactory;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import io.qameta.allure.Issue;
import org.junit.Assert;
import org.junit.Test;

public class SimpleRegistryBootstrapTestCase extends AbstractMuleContextTestCase {

  public static final String TEST_TRANSACTION_FACTORY_CLASS = "org.foo.Connection";

  @Test(expected = ClassNotFoundException.class)
  public void registeringOptionalTransaction() throws Exception {
    createTestRegistryBootstrap(APP);
    muleContext.getTransactionFactoryManager()
        .getTransactionFactoryFor(org.apache.commons.lang3.ClassUtils.getClass(TEST_TRANSACTION_FACTORY_CLASS));
  }

  @Test
  public void existingNotOptionalTransaction() throws Exception {
    createTestRegistryBootstrap(APP);
    TransactionFactory transactionFactoryFor =
        muleContext.getTransactionFactoryManager().getTransactionFactoryFor(FakeTransactionResource.class);
    Assert.assertNotNull(transactionFactoryFor);
  }

  @Test
  public void registerOnlyAppPropertiesType() throws Exception {
    createTestRegistryBootstrap(APP);
    assertThat(((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(String.class), notNullValue());
    assertThat(((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(Properties.class), nullValue());
    assertThat(((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(HashMap.class), nullValue());
    assertThat(((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(ArrayList.class), notNullValue());
  }

  @Test
  public void registerOnlyDomainPropertiesType() throws Exception {
    createTestRegistryBootstrap(DOMAIN);
    assertThat(((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(String.class), nullValue());
    assertThat(((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(Properties.class), notNullValue());
    assertThat(((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(HashMap.class), nullValue());
    assertThat(((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(ArrayList.class), notNullValue());
  }

  @Test
  public void registerOnlyPolicyPropertiesType() throws Exception {
    createTestRegistryBootstrap(POLICY);
    assertThat(((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(String.class), nullValue());
    assertThat(((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(Properties.class), nullValue());
    assertThat(((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(HashMap.class), notNullValue());
    assertThat(((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(ArrayList.class), notNullValue());
  }

  private SimpleRegistryBootstrap createTestRegistryBootstrap(ArtifactType artifactType) throws InitialisationException {
    final Properties properties = new Properties();
    properties.put("1", format("java.lang.String,%s=%s", APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY, APP.getAsString()));
    properties.put("2", format("java.util.Properties,%s=%s", APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY, DOMAIN.getAsString()));
    properties.put("3", format("java.util.HashMap,%s=%s", APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY, POLICY.getAsString()));
    properties.put("4", format("java.util.ArrayList,%s=%s/%s/%s", APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY,
                               APP.getAsString(), DOMAIN.getAsString(), POLICY.getAsString()));
    properties.put("jms.singletx.transaction.resource1", format("%s,optional)", TEST_TRANSACTION_FACTORY_CLASS));
    properties.put("test.singletx.transaction.factory1", FakeTransactionFactory.class.getName());
    properties.put("test.singletx.transaction.resource1", FakeTransactionResource.class.getName());

    final BootstrapServiceDiscoverer bootstrapServiceDiscoverer = new TestBootstrapServiceDiscoverer(properties);
    muleContext.setBootstrapServiceDiscoverer(bootstrapServiceDiscoverer);

    SimpleRegistryBootstrap simpleRegistryBootstrap = new SimpleRegistryBootstrap(artifactType, muleContext);
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
}
