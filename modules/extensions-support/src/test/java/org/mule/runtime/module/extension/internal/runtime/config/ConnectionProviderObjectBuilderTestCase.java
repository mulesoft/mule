/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.extension.api.ExtensionConstants.POOLING_PROFILE_PARAMETER_NAME;
import static org.powermock.api.mockito.PowerMockito.when;

import static java.util.Optional.of;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.runtime.connectivity.ConnectionProviderFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectionProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;


import org.junit.Before;
import org.junit.Test;

public class ConnectionProviderObjectBuilderTestCase {

  private static final PoolingProfile POOLING_PROFILE =
      new PoolingProfile(1, 2, 3, 2, 1);

  private static final PoolingProfile ANOTHER_POOLING_PROFILE =
      new PoolingProfile(2, 5, 1, 1, 2);

  private static final ExtensionModel EXTENSION_MODEL = mock(ExtensionModel.class);
  private static final MuleContext MULE_CONTEXT = mock(MuleContext.class);


  private ConnectionProviderModel connectionProviderModelMock;
  private ConnectionProviderFactory connectionProviderFactoryMock;
  private ConnectionProviderFactoryModelProperty connectionProviderFactoryModelProperty;

  @Before
  public void setup() {
    connectionProviderModelMock = mock(ConnectionProviderModel.class);
    connectionProviderFactoryMock = mock(ConnectionProviderFactory.class);
    connectionProviderFactoryModelProperty = new ConnectionProviderFactoryModelProperty(connectionProviderFactoryMock);
    when(connectionProviderModelMock.getModelProperty(ConnectionProviderFactoryModelProperty.class))
        .thenReturn(of(connectionProviderFactoryModelProperty));
  }

  @Test
  public void poolingProfileFromArguments() {
    ResolverSet resolverSet = new ResolverSet(MULE_CONTEXT);
    TestConnectionProviderObjectBuilderTestCase testConnectionProviderObjectBuilderTestCase =
        new TestConnectionProviderObjectBuilderTestCase(connectionProviderModelMock, resolverSet,
                                                        POOLING_PROFILE, null,
                                                        EXTENSION_MODEL, null,
                                                        MULE_CONTEXT);
    PoolingProfile poolingProfile = testConnectionProviderObjectBuilderTestCase.getPoolingProfile();
    assertThat(poolingProfile, is(POOLING_PROFILE));
  }

  @Test
  public void poolingProfileFromResolverSet() {
    ResolverSet resolverSet = new ResolverSet(MULE_CONTEXT);
    resolverSet.add(POOLING_PROFILE_PARAMETER_NAME, new StaticValueResolver(POOLING_PROFILE));
    TestConnectionProviderObjectBuilderTestCase testConnectionProviderObjectBuilderTestCase =
        new TestConnectionProviderObjectBuilderTestCase(connectionProviderModelMock, resolverSet,
                                                        null, null,
                                                        EXTENSION_MODEL, null,
                                                        MULE_CONTEXT);
    PoolingProfile poolingProfile = testConnectionProviderObjectBuilderTestCase.getPoolingProfile();
    assertThat(poolingProfile, is(POOLING_PROFILE));
  }

  @Test
  public void NoPoolingProfileConfigured() {
    ResolverSet resolverSet = new ResolverSet(MULE_CONTEXT);
    TestConnectionProviderObjectBuilderTestCase testConnectionProviderObjectBuilderTestCase =
        new TestConnectionProviderObjectBuilderTestCase(connectionProviderModelMock, resolverSet,
                                                        null, null,
                                                        EXTENSION_MODEL, null,
                                                        MULE_CONTEXT);
    PoolingProfile poolingProfile = testConnectionProviderObjectBuilderTestCase.getPoolingProfile();
    assertThat(poolingProfile, is(nullValue()));
  }

  @Test
  public void PoolingProfileConfiguredInArgumentsAndResolverSet() {
    ResolverSet resolverSet = new ResolverSet(MULE_CONTEXT);
    resolverSet.add(POOLING_PROFILE_PARAMETER_NAME, new StaticValueResolver(ANOTHER_POOLING_PROFILE));
    TestConnectionProviderObjectBuilderTestCase testConnectionProviderObjectBuilderTestCase =
        new TestConnectionProviderObjectBuilderTestCase(connectionProviderModelMock, resolverSet,
                                                        POOLING_PROFILE, null,
                                                        EXTENSION_MODEL, null,
                                                        MULE_CONTEXT);
    PoolingProfile poolingProfile = testConnectionProviderObjectBuilderTestCase.getPoolingProfile();
    assertThat(poolingProfile, is(POOLING_PROFILE));
  }

  private static class TestConnectionProviderObjectBuilderTestCase extends ConnectionProviderObjectBuilder {

    public TestConnectionProviderObjectBuilderTestCase(ConnectionProviderModel providerModel, ResolverSet resolverSet,
                                                       PoolingProfile poolingProfile, ReconnectionConfig reconnectionConfig,
                                                       ExtensionModel extensionModel, ExpressionManager expressionManager,
                                                       MuleContext muleContext) {
      super(providerModel, resolverSet, poolingProfile, reconnectionConfig, extensionModel, expressionManager, muleContext);
    }

    public PoolingProfile getPoolingProfile() {
      return poolingProfile;
    }
  }


}
