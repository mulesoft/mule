/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.OAUTH_STORE_CONFIG_GROUP_NAME;
import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.OBJECT_STORE_PARAMETER_NAME;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthParameterModelProperty;
import org.mule.runtime.extension.api.runtime.connectivity.ConnectionProviderFactory;
import org.mule.runtime.extension.api.runtime.parameter.HttpParameterPlacement;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectionProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.MapValueResolver;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class BaseOAuthConnectionProviderObjectBuilderTestCase {

  private TestOAuthConnectionProviderObjectBuilder builder;
  private ConnectionProviderModel providerModel;
  private ResolverSet resolverSet;
  private PoolingProfile poolingProfile;
  private ReconnectionConfig reconnectionConfig;
  private ExtensionModel extensionModel;
  private ExpressionManager expressionManager;
  private MuleContext muleContext;
  private CoreEvent event;

  @Before
  public void setUp() {
    providerModel = mock(ConnectionProviderModel.class);
    resolverSet = mock(ResolverSet.class);
    poolingProfile = mock(PoolingProfile.class);
    reconnectionConfig = mock(ReconnectionConfig.class);
    extensionModel = mock(ExtensionModel.class);
    expressionManager = mock(ExpressionManager.class);
    muleContext = mock(MuleContext.class);
    event = mock(CoreEvent.class);

    when(providerModel.getModelProperty(ConnectionProviderFactoryModelProperty.class))
        .thenReturn(Optional.of(new ConnectionProviderFactoryModelProperty(mock(ConnectionProviderFactory.class))));

    builder = new TestOAuthConnectionProviderObjectBuilder(
                                                           providerModel, resolverSet, poolingProfile, reconnectionConfig,
                                                           extensionModel, expressionManager, muleContext);
  }

  @Test
  public void testBuildOAuthObjectStoreConfigWithValidResolver() throws MuleException {
    ValueResolver resolver = mock(ValueResolver.class);
    when(resolverSet.getResolvers()).thenReturn(Collections.singletonMap(OAUTH_STORE_CONFIG_GROUP_NAME, resolver));

    ValueResolvingContext resolvingContext = mock(ValueResolvingContext.class);
    when(resolver.resolve(resolvingContext)).thenReturn(Collections.singletonMap(OBJECT_STORE_PARAMETER_NAME, "storeValue"));

    builder = spy(builder);
    doReturn(resolvingContext).when(builder).getResolvingContextFor(event);

    Optional<OAuthObjectStoreConfig> result = builder.buildOAuthObjectStoreConfig(event);
    assertThat(result.isPresent(), is(true));
    assertThat(result.get().getObjectStoreName(), is("storeValue"));
  }

  @Test
  public void testBuildOAuthObjectStoreConfigWithNullResolver() throws MuleException {
    when(resolverSet.getResolvers()).thenReturn(Collections.emptyMap());

    Optional<OAuthObjectStoreConfig> result = builder.buildOAuthObjectStoreConfig(event);
    assertThat(result.isPresent(), is(false));
  }

  @Test
  public void testSanitizePath() {
    assertThat(builder.sanitizePath("path"), is("/path"));
    assertThat(builder.sanitizePath("/path"), is("/path"));
  }

  @Test
  public void testBuildOAuthObjectStoreConfigWithResolverSetResult() {
    ResolverSetResult result = mock(ResolverSetResult.class);
    when(result.get(OAUTH_STORE_CONFIG_GROUP_NAME))
        .thenReturn(Collections.singletonMap(OBJECT_STORE_PARAMETER_NAME, "storeValue"));

    Optional<OAuthObjectStoreConfig> config = builder.buildOAuthObjectStoreConfig(result);
    assertThat(config.isPresent(), is(true));
    assertThat(config.get().getObjectStoreName(), is("storeValue"));
  }

  @Test
  public void testResolve() throws MuleException {
    ValueResolver resolver = mock(ValueResolver.class);
    ValueResolvingContext context = mock(ValueResolvingContext.class);

    builder = spy(builder);
    doReturn(context).when(builder).getResolvingContextFor(event);
    when(resolver.resolve(context)).thenReturn("resolvedValue");

    Object result = builder.resolve(event, resolver);
    assertThat(result, is("resolvedValue"));
  }

  @Test
  public void testWithCustomParameters() {
    ParameterModel parameterModel = mock(ParameterModel.class);
    OAuthParameterModelProperty property = mock(OAuthParameterModelProperty.class);

    when(providerModel.getAllParameterModels()).thenReturn(Collections.singletonList(parameterModel));
    when(parameterModel.getModelProperty(OAuthParameterModelProperty.class)).thenReturn(Optional.of(property));

    final boolean[] wasCalled = {false};
    builder.withCustomParameters((param, prop) -> wasCalled[0] = true);

    assertThat(wasCalled[0], is(true));
  }

  @Test
  public void testStaticOnly() throws MuleException {
    ValueResolver staticKeyResolver = mock(ValueResolver.class);
    ValueResolver staticValueResolver = mock(ValueResolver.class);
    when(staticKeyResolver.isDynamic()).thenReturn(false);
    when(staticValueResolver.isDynamic()).thenReturn(false);

    List<ValueResolver> keyResolvers = Collections.singletonList(staticKeyResolver);
    List<ValueResolver> valueResolvers = Collections.singletonList(staticValueResolver);

    MapValueResolver resolver = mock(MapValueResolver.class);
    when(resolver.getKeyResolvers()).thenReturn(keyResolvers);
    when(resolver.getValueResolvers()).thenReturn(valueResolvers);

    when(muleContext.getInjector()).thenReturn(mock(Injector.class));

    MapValueResolver result = builder.staticOnly(resolver);
    assertThat(result, is(notNullValue()));
  }

  @Test
  public void testGetCustomParametersWithResolverSetResult() {
    ResolverSetResult result = mock(ResolverSetResult.class);
    when(result.get("customKey")).thenReturn("customValue");

    CustomOAuthParameters params = builder.getCustomParameters(result);
    assertThat(params, is(notNullValue()));
  }

  @Test
  public void testGetCustomParametersFromResolverSetResult() {
    ResolverSetResult resolverSetResult = mock(ResolverSetResult.class);
    ParameterModel parameter1 = mock(ParameterModel.class);
    ParameterModel parameter2 = mock(ParameterModel.class);
    ParameterModel parameter3 = mock(ParameterModel.class);

    OAuthParameterModelProperty property1 = mock(OAuthParameterModelProperty.class);
    OAuthParameterModelProperty property2 = mock(OAuthParameterModelProperty.class);
    OAuthParameterModelProperty property3 = mock(OAuthParameterModelProperty.class);

    when(providerModel.getAllParameterModels()).thenReturn(Arrays.asList(parameter1, parameter2, parameter3));

    when(parameter1.getName()).thenReturn("param1");
    when(parameter2.getName()).thenReturn("param2");
    when(parameter3.getName()).thenReturn("param3");

    when(property1.getRequestAlias()).thenReturn("");
    when(property2.getRequestAlias()).thenReturn("customAlias2");
    when(property3.getRequestAlias()).thenReturn("customAlias3");

    when(property1.getPlacement()).thenReturn(HttpParameterPlacement.QUERY_PARAMS);
    when(property2.getPlacement()).thenReturn(HttpParameterPlacement.HEADERS);
    when(property3.getPlacement()).thenReturn(HttpParameterPlacement.BODY);

    when(parameter1.getModelProperty(OAuthParameterModelProperty.class)).thenReturn(Optional.of(property1));
    when(parameter2.getModelProperty(OAuthParameterModelProperty.class)).thenReturn(Optional.of(property2));
    when(parameter3.getModelProperty(OAuthParameterModelProperty.class)).thenReturn(Optional.of(property3));

    when(resolverSetResult.get("param1")).thenReturn(Collections.singletonMap("key1", "value1"));
    when(resolverSetResult.get("param2")).thenReturn(Arrays.asList("value2a", "value2b"));
    when(resolverSetResult.get("param3")).thenReturn("singleValue");

    CustomOAuthParameters params = builder.getCustomParameters(resolverSetResult);

    assertThat(params.getQueryParams(), hasKey("key1"));
    assertThat(params.getQueryParams().get("key1"), is("value1"));

    assertThat(params.getHeaders(), hasKey("customAlias2"));
    assertThat(params.getHeaders().get("customAlias2"), is("value2a"));

    assertThat(params.getBodyParams(), hasKey("customAlias3"));
    assertThat(params.getBodyParams().get("customAlias3"), is("singleValue"));
  }


  @Test
  public void testGetCustomParametersWithCoreEvent() throws MuleException {
    ValueResolver resolver = mock(ValueResolver.class);
    when(resolverSet.getResolvers()).thenReturn(Collections.singletonMap("customKey", resolver));
    builder = spy(builder);
    doReturn("customValue").when(builder).resolve(event, resolver);

    CustomOAuthParameters params = builder.getCustomParameters(event);
    assertThat(params, is(notNullValue()));
  }

  private static class TestOAuthConnectionProviderObjectBuilder extends BaseOAuthConnectionProviderObjectBuilder<Object> {

    public TestOAuthConnectionProviderObjectBuilder(ConnectionProviderModel providerModel, ResolverSet resolverSet,
                                                    PoolingProfile poolingProfile, ReconnectionConfig reconnectionConfig,
                                                    ExtensionModel extensionModel, ExpressionManager expressionManager,
                                                    MuleContext muleContext) {
      super(providerModel, resolverSet, poolingProfile, reconnectionConfig, extensionModel, expressionManager, muleContext);
    }

    @Override
    protected ValueResolvingContext getResolvingContextFor(CoreEvent event) {
      return mock(ValueResolvingContext.class);
    }
  }
}
