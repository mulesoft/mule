/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getApiMethods;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockClassLoaderModelProperty;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockImplementingType;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockParameters;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.validate;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.visitableMock;

import static java.util.Arrays.asList;
import static java.util.Optional.of;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mockito.quality.Strictness.LENIENT;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.extension.api.annotation.param.DefaultEncoding;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.RefName;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionOperationDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.OperationWrapper;
import org.mule.runtime.module.extension.internal.loader.java.validation.InjectedFieldsModelValidator;
import org.mule.sdk.api.annotation.param.RuntimeVersion;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@SmallTest
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
public class InjectedFieldsModelValidatorTestCase extends AbstractMuleTestCase {

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionModel extensionModel;

  @Mock
  private OperationModel operationModel;

  @Mock
  private SourceModel sourceModel;

  @Mock
  private ConfigurationModel configurationModel;

  @Mock
  private ExtensionTypeDescriptorModelProperty modelProperty;

  @Mock
  private Type type;

  private InjectedFieldsModelValidator validator = new InjectedFieldsModelValidator();

  @BeforeEach
  public void before() {
    mockClassLoaderModelProperty(extensionModel, getClass().getClassLoader());
    when(extensionModel.getName()).thenReturn("dummyExtension");
    visitableMock(operationModel, sourceModel);

    when(extensionModel.getModelProperty(ExtensionTypeDescriptorModelProperty.class)).thenReturn(of(modelProperty));
    when(modelProperty.getType()).thenReturn(type);
    when(type.getDeclaringClass()).thenReturn(of(this.getClass()));
  }

  @Test
  void singleEncodingOperationArgument() {
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    withMethod(operationModel, "singleEncoding");
    validate(extensionModel, validator);
  }

  @Test
  void encodingOperationArgumentWrongType() {
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    withMethod(operationModel, "encodingWrongType");
    assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
  }

  @Test
  void repeatedEncodingOperationArgument() {
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    withMethod(operationModel, "repeatedEncoding");
    assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
  }

  @Test
  void repeatedEncodingOperationArgumentObjectFields() {
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    withMethod(operationModel, "argumentWithRepeatedEncodingFields");
    ParameterModel parameterModel = mock(ParameterModel.class, withSettings().lenient());
    when(parameterModel.getType()).thenReturn(toMetadataType(RepeatedEncoding.class));
    mockParameters(operationModel, parameterModel);
    assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
  }

  @Test
  void repeatedEncodingPojoField() {
    when(extensionModel.getSourceModels()).thenReturn(asList(sourceModel));
    when(sourceModel.getSuccessCallback()).thenReturn(java.util.Optional.empty());
    when(sourceModel.getErrorCallback()).thenReturn(java.util.Optional.empty());
    ParameterModel parameterModel = mock(ParameterModel.class);
    when(parameterModel.getType()).thenReturn(toMetadataType(RepeatedEncoding.class));
    mockParameters(sourceModel, parameterModel);

    mockImplementingType(sourceModel, SourceRepeatedEncoding.class);
    assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
  }

  @Test
  void repeatedEncodingSourceField() {
    when(extensionModel.getSourceModels()).thenReturn(asList(sourceModel));
    when(sourceModel.getSuccessCallback()).thenReturn(java.util.Optional.empty());
    when(sourceModel.getErrorCallback()).thenReturn(java.util.Optional.empty());

    mockImplementingType(sourceModel, SourceRepeatedEncoding.class);
    assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
  }

  @Test
  void repeatedEncodingConfigField() {
    when(extensionModel.getConfigurationModels()).thenReturn(asList(configurationModel));
    mockImplementingType(configurationModel, ConfigRepeatedEncoding.class);

    assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
  }

  @Test
  void legacyApiConfigRef() {
    when(extensionModel.getConfigurationModels()).thenReturn(asList(configurationModel));
    mockImplementingType(configurationModel, ConfigLegacyApiRefName.class);

    validate(extensionModel, validator);
  }

  @Test
  void sdkApiConfigRef() {
    when(extensionModel.getConfigurationModels()).thenReturn(asList(configurationModel));
    mockImplementingType(configurationModel, ConfigSdkApiRefName.class);

    validate(extensionModel, validator);
  }

  @Test
  void repeatedRefNameConfigField() {
    when(extensionModel.getConfigurationModels()).thenReturn(asList(configurationModel));
    mockImplementingType(configurationModel, ConfigRepeatedRefName.class);

    assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
  }

  @Test
  void encodingConfigFieldWrongType() {
    when(extensionModel.getConfigurationModels()).thenReturn(asList(configurationModel));
    mockImplementingType(configurationModel, ConfigEncodingWrongType.class);

    assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
  }

  @Test
  void repeatedRuntimeVersionPojoField() {
    when(extensionModel.getSourceModels()).thenReturn(asList(sourceModel));
    when(sourceModel.getSuccessCallback()).thenReturn(java.util.Optional.empty());
    when(sourceModel.getErrorCallback()).thenReturn(java.util.Optional.empty());
    ParameterModel parameterModel = mock(ParameterModel.class);
    when(parameterModel.getType()).thenReturn(toMetadataType(RepeatedRuntimeVersion.class));
    mockParameters(sourceModel, parameterModel);

    mockImplementingType(sourceModel, SourceRepeatedEncoding.class);
    assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
  }

  @Test
  void repeatedRuntimeVersionConfigField() {
    when(extensionModel.getConfigurationModels()).thenReturn(asList(configurationModel));
    mockImplementingType(configurationModel, RepeatedRuntimeVersion.class);

    assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
  }

  @Test
  void repeatedRuntimeVersionSourceField() {
    when(extensionModel.getSourceModels()).thenReturn(asList(sourceModel));
    when(sourceModel.getSuccessCallback()).thenReturn(java.util.Optional.empty());
    when(sourceModel.getErrorCallback()).thenReturn(java.util.Optional.empty());

    mockImplementingType(sourceModel, SourceRepeatedRuntimeVersion.class);
    assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
  }

  @Test
  void runtimeVersionConfigFieldWrongType() {
    when(extensionModel.getConfigurationModels()).thenReturn(asList(configurationModel));
    mockImplementingType(configurationModel, ConfigRuntimeVersionWrongType.class);

    assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
  }


  public static class RepeatedEncoding {

    @Parameter
    @Optional
    public String someParameter;

    @org.mule.sdk.api.annotation.param.DefaultEncoding
    private String encoding1;

    @DefaultEncoding
    private String encoding2;
  }

  public static class Operations {

    public void repeatedEncoding(@DefaultEncoding String encoding1, @DefaultEncoding String encoding2) {

    }

    public void argumentWithRepeatedEncodingFields(RepeatedEncoding encoding) {

    }

    public void singleEncoding(@org.mule.sdk.api.annotation.param.DefaultEncoding String encoding1) {

    }

    public void encodingWrongType(@DefaultEncoding Integer encoding1) {

    }
  }

  public static class SourceRepeatedEncoding extends Source<String, Object> {

    @DefaultEncoding
    private String encoding1;

    @org.mule.sdk.api.annotation.param.DefaultEncoding
    private String encoding2;

    @Override
    public void onStart(SourceCallback sourceCallback) throws MuleException {

    }

    @Override
    public void onStop() {

    }
  }

  public static class ConfigRepeatedEncoding {

    @DefaultEncoding
    private String encoding1;

    @org.mule.sdk.api.annotation.param.DefaultEncoding
    private String encoding2;
  }

  public static class ConfigEncodingWrongType {

    @DefaultEncoding
    private Boolean encoding1;
  }

  public static class ConfigLegacyApiRefName {

    @RefName
    private String refName;
  }

  public static class ConfigSdkApiRefName {

    @org.mule.sdk.api.annotation.param.RefName
    private String refName;
  }

  public static class ConfigRepeatedRefName {

    @RefName
    private String refName1;

    @org.mule.sdk.api.annotation.param.RefName
    private String refName2;
  }

  public static class RepeatedRuntimeVersion {

    @RuntimeVersion
    private MuleVersion muleVersion1;

    @RuntimeVersion
    private MuleVersion muleVersion2;
  }

  public static class SourceRepeatedRuntimeVersion extends Source<String, Object> {

    @RuntimeVersion
    private MuleVersion encoding1;

    @RuntimeVersion
    private MuleVersion encoding2;

    @Override
    public void onStart(SourceCallback sourceCallback) throws MuleException {}

    @Override
    public void onStop() {}
  }

  public static class ConfigRuntimeVersionWrongType {

    @RuntimeVersion
    private String runtimeVersion;
  }

  private void withMethod(OperationModel operationModel, String operationName) {
    when(operationModel.getModelProperty(ExtensionOperationDescriptorModelProperty.class))
        .thenReturn(getApiMethods(Operations.class).stream()
            .filter(m -> m.getName().equals(operationName))
            .findFirst()
            .map(operationMethod -> new ExtensionOperationDescriptorModelProperty(new OperationWrapper(operationMethod,
                                                                                                       TYPE_LOADER))));
  }
}
