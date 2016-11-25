/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.metadata.api.builder.BaseTypeBuilder.create;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber.DESCRIBER_ID;
import static org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber.TYPE_PROPERTY_NAME;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.EXTENSION_DESCRIPTION;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockClassLoaderModelProperty;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockConfigurationInstance;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockExecutorFactory;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockInterceptors;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockParameters;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.stubRegistryKeys;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.core.transformer.simple.StringToEnum;
import org.mule.runtime.extension.api.declaration.spi.Describer;
import org.mule.runtime.extension.api.manifest.ExtensionManifestBuilder;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.ExtensionFactory;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutor;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutorFactory;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.runtime.module.extension.internal.introspection.enricher.XmlModelEnricher;
import org.mule.runtime.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.ExecutionContextAdapter;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultExtensionManagerTestCase extends AbstractMuleTestCase {

  private static final String MULESOFT = "MuleSoft";
  private static final String OTHER_VENDOR = "OtherVendor";
  private ExtensionManagerAdapter extensionsManager;

  private static final String EXTENSION1_NAME = "extension1";
  private static final String EXTENSION1_CONFIG_NAME = "extension1Config";
  private static final String EXTENSION1_CONFIG_INSTANCE_NAME = "extension1ConfigInstanceName";
  private static final String EXTENSION1_OPERATION_NAME = "extension1OperationName";
  private static final String EXTENSION2_NAME = "extension2";
  private static final String EXTENSION1_VERSION = "3.6.0";
  private static final String EXTENSION2_VERSION = "3.6.0";

  @Mock
  private ExtensionModel extensionModel1;

  @Mock
  private ExtensionModel extensionModel2;

  @Mock
  private ExtensionModel extensionModel3WithRepeatedName;

  private MuleContext muleContext = mockContextWithServices();

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ConfigurationModel extension1ConfigurationModel;

  @Mock
  private OperationModel extension1OperationModel;

  @Mock
  private ExecutionContextAdapter extension1OperationContext;

  @Mock
  private ConfigurationProvider extension1ConfigurationProvider;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ConfigurationInstance extension1ConfigurationInstance = mock(ConfigurationInstance.class);

  @Mock
  private OperationExecutorFactory executorFactory;

  @Mock
  private OperationExecutor executor;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private Event event;

  private ClassLoader classLoader;

  private final Object configInstance = new Object();

  @Before
  public void before() throws InitialisationException {
    DefaultExtensionManager extensionsManager = new DefaultExtensionManager();
    extensionsManager.setMuleContext(muleContext);
    extensionsManager.initialise();
    this.extensionsManager = extensionsManager;

    when(extensionModel1.getName()).thenReturn(EXTENSION1_NAME);
    mockClassLoaderModelProperty(extensionModel1, getClass().getClassLoader());

    when(extensionModel1.getConfigurationModels()).thenReturn(asList(extension1ConfigurationModel));
    when(extensionModel2.getName()).thenReturn(EXTENSION2_NAME);
    when(extensionModel3WithRepeatedName.getName()).thenReturn(EXTENSION2_NAME);

    when(extensionModel1.getVendor()).thenReturn(MULESOFT);
    when(extensionModel2.getVendor()).thenReturn(MULESOFT);
    when(extensionModel3WithRepeatedName.getVendor()).thenReturn(OTHER_VENDOR);

    when(extensionModel1.getVersion()).thenReturn(EXTENSION1_VERSION);
    when(extensionModel2.getVersion()).thenReturn(EXTENSION2_VERSION);
    mockClassLoaderModelProperty(extensionModel1, getClass().getClassLoader());
    when(extensionModel3WithRepeatedName.getVersion()).thenReturn(EXTENSION2_VERSION);

    when(extension1ConfigurationModel.getName()).thenReturn(EXTENSION1_CONFIG_NAME);
    mockConfigurationInstance(extension1ConfigurationModel, configInstance);
    mockInterceptors(extension1ConfigurationModel, null);
    when(extension1ConfigurationModel.getOperationModels()).thenReturn(ImmutableList.of());
    when(extension1ConfigurationModel.getSourceModels()).thenReturn(ImmutableList.of());
    mockParameters(extension1ConfigurationModel);
    mockConfigurationInstance(extension1ConfigurationModel, configInstance);

    when(extensionModel1.getConfigurationModel(EXTENSION1_CONFIG_NAME)).thenReturn(Optional.of(extension1ConfigurationModel));
    when(extensionModel1.getOperationModel(EXTENSION1_OPERATION_NAME)).thenReturn(Optional.of(extension1OperationModel));
    when(extension1OperationModel.getName()).thenReturn(EXTENSION1_OPERATION_NAME);

    when(extension1ConfigurationInstance.getValue()).thenReturn(configInstance);
    when(extension1ConfigurationInstance.getModel()).thenReturn(extension1ConfigurationModel);
    when(extension1ConfigurationInstance.getName()).thenReturn(EXTENSION1_CONFIG_INSTANCE_NAME);

    when(extension1ConfigurationProvider.get(event)).thenReturn(extension1ConfigurationInstance);
    when(extension1ConfigurationProvider.getConfigurationModel()).thenReturn(extension1ConfigurationModel);
    when(extension1ConfigurationProvider.getExtensionModel()).thenReturn(extensionModel1);
    when(extension1ConfigurationProvider.getName()).thenReturn(EXTENSION1_CONFIG_INSTANCE_NAME);

    mockExecutorFactory(extension1OperationModel, executorFactory);
    when(executorFactory.createExecutor(extension1OperationModel)).thenReturn(executor);

    classLoader = getClass().getClassLoader();
    registerExtensions(extensionModel1, extensionModel2, extensionModel3WithRepeatedName);

    stubRegistryKeys(muleContext, EXTENSION1_CONFIG_INSTANCE_NAME, EXTENSION1_OPERATION_NAME, EXTENSION1_NAME);
  }

  private void registerExtensions(ExtensionModel... extensionModels) {
    Arrays.stream(extensionModels).forEach(extensionsManager::registerExtension);
  }

  @Test
  public void getExtensions() {
    testEquals(asList(extensionModel1, extensionModel2), extensionsManager.getExtensions());
  }

  @Test
  public void getExtensionsByName() {
    Optional<ExtensionModel> extension = extensionsManager.getExtension(EXTENSION1_NAME);
    assertThat(extension.isPresent(), is(true));
    assertThat(extension.get(), is(sameInstance(extensionModel1)));
  }

  @Test
  public void contextClassLoaderKept() {
    assertThat(classLoader, sameInstance(Thread.currentThread().getContextClassLoader()));
  }

  @Test
  public void contextClassLoaderKeptAfterException() {
    ExtensionModel extensionModel = mock(ExtensionModel.class);
    when(extensionModel.getName()).thenThrow(new RuntimeException());
    try {
      extensionsManager.registerExtension(extensionModel);
      fail("was expecting an exception");
    } catch (RuntimeException e) {
      assertThat(classLoader, sameInstance(Thread.currentThread().getContextClassLoader()));
    }
  }

  @Test
  public void getConfigurationByName() throws Exception {
    registerConfigurationProvider();

    ConfigurationInstance configurationInstance =
        extensionsManager.getConfiguration(EXTENSION1_CONFIG_INSTANCE_NAME, event);
    assertThat(configurationInstance.getValue(), is(sameInstance(configInstance)));
  }

  @Test
  public void getConfigurationThroughDefaultConfig() throws Exception {
    registerConfigurationProvider();

    ConfigurationInstance configInstance = extensionsManager.getConfiguration(extensionModel1, event);
    assertThat(configInstance.getValue(), is(sameInstance(this.configInstance)));
  }

  @Test
  public void getConfigurationThroughImplicitConfiguration() throws Exception {
    when(extension1ConfigurationModel.getModelProperty(ParameterGroupModelProperty.class)).thenReturn(Optional.empty());
    registerConfigurationProvider();
    ConfigurationInstance configInstance = extensionsManager.getConfiguration(extensionModel1, event);
    assertThat(configInstance.getValue(), is(sameInstance(this.configInstance)));
  }

  @Test
  public void getOperationExecutorThroughImplicitConfigurationConcurrently() throws Exception {
    final int threadCount = 2;
    final CountDownLatch joinerLatch = new CountDownLatch(threadCount);

    MuleRegistry registry = muleContext.getRegistry();
    when(extension1ConfigurationModel.getModelProperty(ParameterGroupModelProperty.class)).thenReturn(Optional.empty());
    when(registry.lookupObjects(ConfigurationProvider.class)).thenReturn(emptyList());

    doAnswer(invocation -> {
      when(muleContext.getRegistry().lookupObjects(ConfigurationProvider.class))
          .thenReturn(asList((ConfigurationProvider) invocation.getArguments()[1]));
      new Thread(() -> extensionsManager.getConfiguration(extensionModel1, event)).start();
      joinerLatch.countDown();

      return null;
    }).when(registry).registerObject(anyString(), anyObject());

    ConfigurationInstance configurationInstance = extensionsManager.getConfiguration(extensionModel1, event);
    joinerLatch.countDown();
    assertThat(joinerLatch.await(5, TimeUnit.SECONDS), is(true));
    assertThat(configurationInstance.getValue(), is(sameInstance(configInstance)));
  }

  @Test(expected = IllegalStateException.class)
  public void getOperationExecutorWithNotImplicitConfig() {
    when(muleContext.getRegistry().lookupObjects(ConfigurationProvider.class)).thenReturn(emptyList());
    makeExtension1ConfigurationNotImplicit();

    extensionsManager.getConfiguration(extensionModel1, event);
  }

  @Test
  public void registerTwoExtensionsWithTheSameNameButDifferentVendor() {
    registerExtensions(extensionModel2, extensionModel3WithRepeatedName);
    List<ExtensionModel> extensionModels = new ArrayList<>(extensionsManager.getExtensions());
    List<String> extensionNameList =
        extensionModels.stream().map(ExtensionModel::getName).distinct().collect(Collectors.toList());
    List<String> extensionVendorList =
        extensionModels.stream().map(ExtensionModel::getVendor).distinct().collect(Collectors.toList());

    assertThat(extensionModels.size(), is(2));
    assertThat(extensionNameList.size(), is(2));
    assertThat(extensionVendorList.size(), is(1));
  }

  @Test
  public void ignoresRegisteringAlreadyRegisteredExtensions() {
    final int registeredExtensionsCount = extensionsManager.getExtensions().size();
    registerExtensions(extensionModel1, extensionModel1, extensionModel1);
    assertThat(extensionsManager.getExtensions(), hasSize(registeredExtensionsCount));
  }

  @Test
  public void registerFromManifest() throws Exception {
    final ServiceRegistry serviceRegistry = mock(ServiceRegistry.class);
    when(serviceRegistry.lookupProviders(anyObject(), anyObject())).thenReturn(asList(new XmlModelEnricher()));

    final String version = "4.0.0";
    ExtensionManifestBuilder builder =
        new ExtensionManifestBuilder().setName(HEISENBERG).setDescription(EXTENSION_DESCRIPTION).setVersion(version);
    builder.withDescriber().setId(DESCRIBER_ID).addProperty(TYPE_PROPERTY_NAME, HeisenbergExtension.class.getName());

    Describer describer = new DescriberResolver().resolve(builder.build(), classLoader);
    final DefaultDescribingContext context = new DefaultDescribingContext(classLoader);

    ExtensionFactory extensionFactory = new DefaultExtensionFactory(serviceRegistry, muleContext.getExecutionClassLoader());

    ExtensionModel extensionModel =
        withContextClassLoader(classLoader, () -> extensionFactory.createFrom(describer.describe(context), context));

    extensionsManager.registerExtension(extensionModel);

    Optional<ExtensionModel> registered = extensionsManager.getExtension(HEISENBERG);
    assertThat(registered.isPresent(), is(true));

    final ExtensionModel registeredExtension = registered.get();
    assertThat(registeredExtension.getName(), is(HEISENBERG));
    assertThat(registeredExtension.getVersion(), is(version));
  }

  @Test
  public void enumTransformer() throws Exception {
    DefaultExtensionManager extensionsManager = new DefaultExtensionManager();
    extensionsManager.setMuleContext(muleContext);
    extensionsManager.initialise();

    ParameterModel parameter = mock(ParameterModel.class);
    when(parameter.getType()).thenReturn(toMetadataType(TimeUnit.class));

    ParameterModel parameterOfRepeatedEnumType = mock(ParameterModel.class);
    when(parameterOfRepeatedEnumType.getType()).thenReturn(toMetadataType(TimeUnit.class));

    mockParameters(extension1ConfigurationModel, parameter, parameterOfRepeatedEnumType);
    extensionsManager.registerExtension(extensionModel1);

    verify(muleContext.getRegistry()).registerTransformer(any(StringToEnum.class));
  }

  @Test
  public void enumCollectionTransformer() throws Exception {
    DefaultExtensionManager extensionsManager = new DefaultExtensionManager();
    extensionsManager.setMuleContext(muleContext);
    extensionsManager.initialise();

    ParameterModel parameter = mock(ParameterModel.class);
    when(parameter.getType())
        .thenReturn(create(JAVA).arrayType().of(toMetadataType(TimeUnit.class)).build());

    mockParameters(extension1ConfigurationModel, parameter);
    extensionsManager.registerExtension(extensionModel1);

    verify(muleContext.getRegistry()).registerTransformer(any(StringToEnum.class));
  }

  private void makeExtension1ConfigurationNotImplicit() {
    ParameterModel parameterModel1 = mock(ParameterModel.class);
    when(parameterModel1.isRequired()).thenReturn(true);

    mockParameters(extension1ConfigurationModel, parameterModel1, parameterModel1);
    mockConfigurationInstance(extension1ConfigurationModel, configInstance);
  }

  private void testEquals(Collection<ExtensionModel> expected, Collection<ExtensionModel> obtained) {
    assertThat(obtained.size(), is(expected.size()));
    Iterator<ExtensionModel> expectedIterator = expected.iterator();
    Iterator<ExtensionModel> obtainedIterator = expected.iterator();

    while (expectedIterator.hasNext()) {
      assertThat(obtainedIterator.hasNext(), is(true));
      testEquals(expectedIterator.next(), obtainedIterator.next());
    }
  }

  private void testEquals(ExtensionModel expected, ExtensionModel obtained) {
    assertThat(obtained.getName(), equalTo(expected.getName()));
    assertThat(obtained.getVersion(), equalTo(expected.getVersion()));
  }

  private void registerConfigurationProvider() throws RegistrationException {
    extensionsManager.registerConfigurationProvider(extension1ConfigurationProvider);
    verify(muleContext.getRegistry()).registerObject(extension1ConfigurationProvider.getName(), extension1ConfigurationProvider);
    when(muleContext.getRegistry().lookupObjects(ConfigurationProvider.class))
        .thenReturn(asList(extension1ConfigurationProvider));
    when(muleContext.getRegistry().get(extension1ConfigurationProvider.getName())).thenReturn(extension1ConfigurationProvider);
  }
}
