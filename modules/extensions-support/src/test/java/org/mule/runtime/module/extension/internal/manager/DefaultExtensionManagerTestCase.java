/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager;

import static org.mule.metadata.api.builder.BaseTypeBuilder.create;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTION_MANAGER;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager.EXTENSION_JVM_ENFORCEMENT_PROPERTY;
import static org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager.JVM_ENFORCEMENT_DISABLED;
import static org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager.JVM_ENFORCEMENT_LOOSE;
import static org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager.JVM_ENFORCEMENT_STRICT;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getImplicitConfigurationProviderName;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import static org.mule.tck.util.MuleContextUtils.registerIntoMockContext;
import static org.mule.tck.util.MuleContextUtils.verifyExactRegistration;
import static org.mule.tck.util.MuleContextUtils.verifyRegistration;
import static org.mule.tck.util.MuleContextUtils.whenRegistration;
import static org.mule.test.allure.AllureConstants.Sdk.SDK;
import static org.mule.test.allure.AllureConstants.Sdk.SupportedJavaVersions.ENFORCE_EXTENSION_JAVA_VERSION;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockClassLoaderModelProperty;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockConfigurationInstance;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockExecutorFactory;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockParameters;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockSupportedJavaVersions;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.stubRegistryKeys;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.visitableMock;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.junit.Assert.fail;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.core.internal.transformer.simple.StringToEnum;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.extension.api.property.ClassLoaderModelProperty;
import org.mule.runtime.extension.api.property.ImplicitConfigNameModelProperty;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.connectivity.ConnectionProviderFactory;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutorFactory;
import org.mule.runtime.metadata.internal.MuleMetadataService;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectionProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.manager.jdk.LooseExtensionJdkValidator;
import org.mule.runtime.module.extension.internal.manager.jdk.NullExtensionJdkValidator;
import org.mule.runtime.module.extension.internal.manager.jdk.StrictExtensionJdkValidator;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@SmallTest
@Feature(SDK)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
public class DefaultExtensionManagerTestCase extends AbstractMuleTestCase {

  private static final String MULESOFT = "MuleSoft";
  private static final String OTHER_VENDOR = "OtherVendor";
  private static final XmlDslModel XML_DSL_MODEL = XmlDslModel.builder().setPrefix("extension-prefix").build();
  private ExtensionManager extensionsManager;

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

  private MuleContext muleContext;

  @Mock(answer = RETURNS_DEEP_STUBS, lenient = true)
  private ConfigurationModel extension1ConfigurationModel;

  @Mock(answer = RETURNS_DEEP_STUBS, lenient = true)
  private ConnectionProviderModel connectionProviderModel;

  @Mock(answer = RETURNS_DEEP_STUBS, lenient = true)
  private ConnectionManagerAdapter connectionManagerAdapter;

  @Mock
  private OperationModel extension1OperationModel;

  @Mock
  private ExecutionContextAdapter extension1OperationContext;

  @Mock
  private ConfigurationProvider extension1ConfigurationProvider;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private final ConfigurationInstance extension1ConfigurationInstance = mock(ConfigurationInstance.class);

  @Mock
  private CompletableComponentExecutorFactory executorFactory;

  @Mock
  private CompletableComponentExecutor executor;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private CoreEvent event;

  private ClassLoader classLoader;

  private final Object configInstance = new Object();

  @BeforeEach
  public void before() throws MuleException {
    muleContext = mockContextWithServices();
    when(muleContext.getArtifactType()).thenReturn(APP);
    DefaultExtensionManager extensionsManager = new DefaultExtensionManager();
    initialiseIfNeeded(extensionsManager, true, muleContext);
    this.extensionsManager = extensionsManager;

    mockClassLoaderModelProperty(extensionModel1, getClass().getClassLoader());
    mockClassLoaderModelProperty(extensionModel2, getClass().getClassLoader());
    mockClassLoaderModelProperty(extensionModel3WithRepeatedName, getClass().getClassLoader());
    mockSupportedJavaVersions(extensionModel1, extensionModel2, extensionModel3WithRepeatedName);

    when(extensionModel1.getName()).thenReturn(EXTENSION1_NAME);
    mockClassLoaderModelProperty(extensionModel1, getClass().getClassLoader());

    when(extensionModel1.getXmlDslModel()).thenReturn(XML_DSL_MODEL);
    when(extensionModel2.getXmlDslModel()).thenReturn(XML_DSL_MODEL);

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
    when(extension1ConfigurationModel.getOperationModels()).thenReturn(ImmutableList.of(extension1OperationModel));
    when(extension1ConfigurationModel.getSourceModels()).thenReturn(ImmutableList.of());
    when(extension1ConfigurationModel.getConnectionProviders()).thenReturn(asList(connectionProviderModel));
    when(connectionProviderModel.getAllParameterModels()).thenReturn(emptyList());

    when(connectionProviderModel.getModelProperty(ConnectionProviderFactoryModelProperty.class))
        .thenReturn(Optional.of(new ConnectionProviderFactoryModelProperty(mock(ConnectionProviderFactory.class,
                                                                                RETURNS_DEEP_STUBS))));
    mockParameters(extension1ConfigurationModel);
    mockConfigurationInstance(extension1ConfigurationModel, configInstance);

    when(extensionModel1.getConfigurationModel(EXTENSION1_CONFIG_NAME)).thenReturn(of(extension1ConfigurationModel));
    when(extensionModel1.getOperationModel(EXTENSION1_OPERATION_NAME)).thenReturn(of(extension1OperationModel));
    when(extension1OperationModel.getName()).thenReturn(EXTENSION1_OPERATION_NAME);
    when(extensionModel1.getFunctionModels()).thenReturn(emptyList());
    when(extensionModel1.getConstructModels()).thenReturn(emptyList());
    when(extension1ConfigurationInstance.getValue()).thenReturn(configInstance);
    when(extension1ConfigurationInstance.getModel()).thenReturn(extension1ConfigurationModel);
    when(extension1ConfigurationInstance.getName()).thenReturn(EXTENSION1_CONFIG_INSTANCE_NAME);

    when(extension1ConfigurationProvider.get(event)).thenReturn(extension1ConfigurationInstance);
    when(extension1ConfigurationProvider.getConfigurationModel()).thenReturn(extension1ConfigurationModel);
    when(extension1ConfigurationProvider.getExtensionModel()).thenReturn(extensionModel1);
    when(extension1ConfigurationProvider.getName()).thenReturn(EXTENSION1_CONFIG_INSTANCE_NAME);

    visitableMock(extension1OperationModel);
    mockExecutorFactory(extension1OperationModel, executorFactory);
    when(executorFactory.createExecutor(same(extension1OperationModel), anyMap())).thenReturn(executor);

    classLoader = getClass().getClassLoader();
    registerExtensions(extensionModel1, extensionModel2, extensionModel3WithRepeatedName);

    stubRegistryKeys(muleContext, EXTENSION1_CONFIG_INSTANCE_NAME, EXTENSION1_OPERATION_NAME, EXTENSION1_NAME);

    registerIntoMockContext(muleContext, OBJECT_CONNECTION_MANAGER, mock(ConnectionManagerAdapter.class));
    registerIntoMockContext(muleContext, MuleMetadataService.class, mock(MuleMetadataService.class));

    muleContext.getInjector().inject(extensionsManager);
  }

  private void registerExtensions(ExtensionModel... extensionModels) {
    Arrays.stream(extensionModels).forEach(extension -> {
      when(extension.getModelProperty(ClassLoaderModelProperty.class)).thenReturn(empty());
      extensionsManager.registerExtension(extension);
    });
  }

  @Test
  void getExtensions() {
    testEquals(asList(extensionModel1, extensionModel2), extensionsManager.getExtensions());
  }

  @Test
  void getExtensionsByName() {
    Optional<ExtensionModel> extension = extensionsManager.getExtension(EXTENSION1_NAME);
    assertThat(extension.isPresent(), is(true));
    assertThat(extension.get(), is(sameInstance(extensionModel1)));
  }

  @Test
  void contextClassLoaderKept() {
    assertThat(classLoader, sameInstance(Thread.currentThread().getContextClassLoader()));
  }

  @Test
  void contextClassLoaderKeptAfterException() {
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
  void getConfigurationByName() throws Exception {
    registerConfigurationProvider();

    ConfigurationInstance configurationInstance =
        extensionsManager.getConfiguration(EXTENSION1_CONFIG_INSTANCE_NAME, event);
    assertThat(configurationInstance.getValue(), is(sameInstance(configInstance)));
  }

  @Test
  void getConfigurationThroughImplicitConfiguration() throws Exception {
    registerIntoMockContext(muleContext, getImplicitConfigurationProviderName(extensionModel1,
                                                                              extension1ConfigurationModel,
                                                                              muleContext.getArtifactType(), muleContext.getId(),
                                                                              feature -> false),
                            extension1ConfigurationProvider);
    when(extension1ConfigurationModel.getModelProperty(ParameterGroupModelProperty.class)).thenReturn(empty());
    registerConfigurationProvider();
    final Optional<ImplicitConfigNameModelProperty> implicitConfigName =
        of(new ImplicitConfigNameModelProperty(extension1ConfigurationModel.getName()));
    when(extension1OperationModel.getModelProperty(ImplicitConfigNameModelProperty.class))
        .thenReturn(implicitConfigName);

    Optional<ConfigurationInstance> configInstance =
        extensionsManager.getConfiguration(extensionModel1, extension1OperationModel, event);
    assertThat(configInstance.isPresent(), is(true));
    assertThat(configInstance.get().getValue(), is(sameInstance(this.configInstance)));
  }

  @Test
  void getOperationExecutorThroughImplicitConfigurationConcurrently() throws Exception {
    final int threadCount = 2;
    final CountDownLatch joinerLatch = new CountDownLatch(threadCount);

    when(extension1ConfigurationModel.getModelProperty(ParameterGroupModelProperty.class)).thenReturn(empty());
    whenRegistration(muleContext, invocation -> {
      registerIntoMockContext(muleContext, getImplicitConfigurationProviderName(extensionModel1,
                                                                                extension1ConfigurationModel,
                                                                                muleContext.getArtifactType(),
                                                                                muleContext.getId(), feature -> false),
                              extension1ConfigurationProvider);
      new Thread(() -> extensionsManager.getConfiguration(extensionModel1, extension1OperationModel, event)).start();
      joinerLatch.countDown();
      return null;
    });
    final Optional<ImplicitConfigNameModelProperty> implicitConfigName =
        of(new ImplicitConfigNameModelProperty(extension1ConfigurationModel.getName()));
    when(extension1OperationModel.getModelProperty(ImplicitConfigNameModelProperty.class))
        .thenReturn(implicitConfigName);

    Optional<ConfigurationInstance> configurationInstance = extensionsManager.getConfiguration(extensionModel1,
                                                                                               extension1OperationModel,
                                                                                               event);
    joinerLatch.countDown();
    assertThat(configurationInstance.isPresent(), is(true));
    assertThat(joinerLatch.await(5, TimeUnit.SECONDS), is(true));
    assertThat(configurationInstance.get().getValue(), is(sameInstance(configInstance)));
  }

  @Test
  void getOperationExecutorWithNotImplicitConfig() {
    makeExtension1ConfigurationNotImplicit();

    assertThrows(IllegalStateException.class,
                 () -> extensionsManager.getConfiguration(extensionModel1, extension1OperationModel, event));
  }

  @Test
  void registerTwoExtensionsWithTheSameNameButDifferentVendor() {
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
  void ignoresRegisteringAlreadyRegisteredExtensions() {
    final int registeredExtensionsCount = extensionsManager.getExtensions().size();
    registerExtensions(extensionModel1, extensionModel1, extensionModel1);
    assertThat(extensionsManager.getExtensions(), hasSize(registeredExtensionsCount));
  }

  @Test
  void enumTransformer() throws Exception {
    DefaultExtensionManager extensionsManager = new DefaultExtensionManager();
    initialiseIfNeeded(extensionsManager, true, muleContext);

    ParameterModel parameter = mock(ParameterModel.class);
    when(parameter.getType()).thenReturn(toMetadataType(TimeUnit.class));

    ParameterModel parameterOfRepeatedEnumType = mock(ParameterModel.class);
    when(parameterOfRepeatedEnumType.getType()).thenReturn(toMetadataType(TimeUnit.class));

    mockParameters(extension1ConfigurationModel, parameter, parameterOfRepeatedEnumType);
    extensionsManager.registerExtension(extensionModel1);

    verifyRegistration(muleContext, StringToEnum.class);
  }

  @Test
  void enumCollectionTransformer() throws Exception {
    DefaultExtensionManager extensionsManager = new DefaultExtensionManager();
    initialiseIfNeeded(extensionsManager, true, muleContext);

    ParameterModel parameter = mock(ParameterModel.class);
    when(parameter.getType())
        .thenReturn(create(JAVA).arrayType().of(toMetadataType(TimeUnit.class)).build());

    mockParameters(extension1ConfigurationModel, parameter);
    extensionsManager.registerExtension(extensionModel1);

    verifyRegistration(muleContext, StringToEnum.class);
  }

  @Test
  @Story(ENFORCE_EXTENSION_JAVA_VERSION)
  void resolveJdkValidator() {
    DefaultExtensionManager extensionManager = (DefaultExtensionManager) extensionsManager;

    // assert default value
    assertThat(extensionManager.getExtensionJdkValidator(), is(instanceOf(StrictExtensionJdkValidator.class)));

    try {
      setProperty(EXTENSION_JVM_ENFORCEMENT_PROPERTY, JVM_ENFORCEMENT_STRICT);
      extensionManager.resolveJdkValidator();
      assertThat(extensionManager.getExtensionJdkValidator(), is(instanceOf(StrictExtensionJdkValidator.class)));

      setProperty(EXTENSION_JVM_ENFORCEMENT_PROPERTY, JVM_ENFORCEMENT_LOOSE);
      extensionManager.resolveJdkValidator();
      assertThat(extensionManager.getExtensionJdkValidator(), is(instanceOf(LooseExtensionJdkValidator.class)));

      setProperty(EXTENSION_JVM_ENFORCEMENT_PROPERTY, JVM_ENFORCEMENT_DISABLED);
      extensionManager.resolveJdkValidator();
      assertThat(extensionManager.getExtensionJdkValidator(), is(instanceOf(NullExtensionJdkValidator.class)));
    } finally {
      clearProperty(EXTENSION_JVM_ENFORCEMENT_PROPERTY);
    }
  }

  private void makeExtension1ConfigurationNotImplicit() {
    ParameterModel parameterModel1 = mock(ParameterModel.class);
    when(parameterModel1.isRequired()).thenReturn(true);

    mockParameters(extension1ConfigurationModel, parameterModel1, parameterModel1);
    mockConfigurationInstance(extension1ConfigurationModel, configInstance);
  }

  private void testEquals(Collection<ExtensionModel> expected, Collection<ExtensionModel> obtained) {
    assertThat(obtained.size(), is(expected.size()));
    Iterator<ExtensionModel> obtainedIterator = expected.iterator();

    for (ExtensionModel element : expected) {
      assertThat(obtainedIterator.hasNext(), is(true));
      testEquals(element, obtainedIterator.next());
    }
  }

  private void testEquals(ExtensionModel expected, ExtensionModel obtained) {
    assertThat(obtained.getName(), equalTo(expected.getName()));
    assertThat(obtained.getVersion(), equalTo(expected.getVersion()));
  }

  private void registerConfigurationProvider() throws RegistrationException {
    extensionsManager.registerConfigurationProvider(extension1ConfigurationProvider);
    verifyExactRegistration(muleContext, extension1ConfigurationProvider.getName(), extension1ConfigurationProvider);
    registerIntoMockContext(muleContext, ConfigurationProvider.class,
                            extension1ConfigurationProvider);
    registerIntoMockContext(muleContext, extension1ConfigurationProvider.getName(),
                            extension1ConfigurationProvider);
  }
}
