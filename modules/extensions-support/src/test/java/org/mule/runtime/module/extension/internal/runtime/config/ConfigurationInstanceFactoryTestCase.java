/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockConfigurationInstance;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.source.SourceCallbackModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectivityModelProperty;
import org.mule.runtime.module.extension.internal.runtime.execution.ConfigurationObjectBuilderTestCase;
import org.mule.runtime.module.extension.internal.runtime.execution.ConfigurationObjectBuilderTestCase.TestConfig;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Kiwi;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ConfigurationInstanceFactoryTestCase extends AbstractMuleTestCase {

  private static final String CONFIG_NAME = "config";
  private static final String ENCODING = "UTF-8";

  @Mock(answer = RETURNS_DEEP_STUBS, lenient = true)
  private ConfigurationModel configurationModel;

  @Mock(lenient = true)
  private OperationModel operationModel;

  @Mock(lenient = true)
  private ExtensionModel extensionModel;

  @Mock(lenient = true)
  private SourceModel sourceModel;

  @Mock(lenient = true)
  private SourceCallbackModel sourceCallbackModel;

  @Mock(lenient = true)
  private ComponentModel componentModel;

  @Mock(answer = RETURNS_DEEP_STUBS, lenient = true)
  private CoreEvent event;

  @Mock(lenient = true)
  private ConnectionProviderValueResolver<Object> connectionProviderValueResolver;

  @Mock(answer = RETURNS_DEEP_STUBS, lenient = true)
  private ExpressionManager expressionManager;

  @Mock(answer = RETURNS_DEEP_STUBS, lenient = true)
  private MuleContext muleContext;

  private ResolverSet resolverSet;
  private ConfigurationInstanceFactory<TestConfig> factory;

  @Before
  public void before() throws Exception {
    mockConfigurationInstance(configurationModel, new TestConfig());
    when(configurationModel.getOperationModels()).thenReturn(ImmutableList.of());
    when(configurationModel.getSourceModels()).thenReturn(ImmutableList.of());
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    when(extensionModel.getSourceModels()).thenReturn(asList(sourceModel));
    when(operationModel.getModelProperty(ConnectivityModelProperty.class))
        .thenReturn(of(new ConnectivityModelProperty(Banana.class)));
    when(sourceModel.getModelProperty(ConnectivityModelProperty.class))
        .thenReturn(of(new ConnectivityModelProperty(Banana.class)));
    when(sourceModel.getErrorCallback()).thenReturn(of(sourceCallbackModel));
    when(sourceModel.getSuccessCallback()).thenReturn(of(sourceCallbackModel));
    when(muleContext.getConfiguration().getDefaultEncoding()).thenReturn(ENCODING);

    resolverSet = ConfigurationObjectBuilderTestCase.createResolverSet();
    factory = new ConfigurationInstanceFactory<>(extensionModel, configurationModel, resolverSet, expressionManager, muleContext);
  }

  @Test
  public void createFromEvent() throws Exception {
    ConfigurationInstance configurationInstance =
        factory.createConfiguration(CONFIG_NAME, event, connectionProviderValueResolver);

    assertConfiguration(configurationInstance);
  }

  @Test
  public void createFromResolverSetResult() throws Exception {
    ResolverSetResult result = ResolverSetResult.newBuilder().build();
    ConfigurationInstance configurationInstance = factory.createConfiguration(CONFIG_NAME, result, event, empty());

    assertConfiguration(configurationInstance);
    assertThat(configurationInstance.getConnectionProvider().isPresent(), is(false));
  }

  private void assertConfiguration(ConfigurationInstance configurationInstance) {
    assertThat(configurationInstance, is(notNullValue()));
    assertThat(configurationInstance.getName(), is(CONFIG_NAME));
    assertThat(configurationInstance.getModel(), is(sameInstance(configurationModel)));
    assertThat(configurationInstance.getValue(), is(instanceOf(TestConfig.class)));
  }

  public static class InvalidConfigTestConnectionProvider implements ConnectionProvider<Banana> {

    @Override
    public Banana connect() throws ConnectionException {
      return new Banana();
    }

    @Override
    public void disconnect(Banana banana) {

    }

    @Override
    public ConnectionValidationResult validate(Banana banana) {
      return ConnectionValidationResult.success();
    }
  }


  public static class InvalidConnectionTypeProvider implements ConnectionProvider<Kiwi> {

    @Override
    public Kiwi connect() throws ConnectionException {
      return new Kiwi();
    }

    @Override
    public void disconnect(Kiwi kiwi) {

    }

    @Override
    public ConnectionValidationResult validate(Kiwi kiwi) {
      return ConnectionValidationResult.success();
    }
  }
}
