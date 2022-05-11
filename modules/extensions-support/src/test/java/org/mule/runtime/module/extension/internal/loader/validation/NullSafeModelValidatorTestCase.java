/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockParameters;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.validate;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.module.extension.internal.loader.java.validation.NullSafeModelValidator;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class NullSafeModelValidatorTestCase extends AbstractMuleTestCase {

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionModel extensionModel;

  @Mock(lenient = true)
  private OperationModel operationModel;

  @Mock(lenient = true)
  private ParameterModel parameterModel;

  private ExtensionModelValidator validator = new NullSafeModelValidator();

  @Before
  public void before() {
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    mockParameters(operationModel, parameterModel);
    when(extensionModel.getImportedTypes()).thenReturn(emptySet());
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void collectionWithImplementingTypeUsingSdkApi() {
    when(parameterModel.getType()).thenReturn(toMetadataType(CollectionWithDefaultImplementingTypeUsingSdkApi.class));
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void dictionaryWithImplementingTypeUsingSdkApi() {
    when(parameterModel.getType()).thenReturn(toMetadataType(DictionaryWithDefaultImplementingTypeUsingSdkApi.class));
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void implementingTypeIsNotAssignableUsingSdkApi() {
    when(parameterModel.getType()).thenReturn(toMetadataType(ImplementingTypeNotAssignableUsingSdkApi.class));
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void basicTypeFieldUsingSdkApi() {
    when(parameterModel.getType()).thenReturn(toMetadataType(BasicTypeWithNullSafeUsingSdkApi.class));
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void abstractFieldTypeUsingSdkApi() {
    when(parameterModel.getType()).thenReturn(toMetadataType(AbstractNullSafeTypeWithoutOverrideUsingSdkApi.class));
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void abstractImplementingTypeUsingSdkApi() {
    when(parameterModel.getType()).thenReturn(toMetadataType(AbstractNullSafeTypeWithAbstractOverrideUsingSdkApi.class));
    validate(extensionModel, validator);
  }

  @Test
  public void validModelUsingSdkApi() {
    when(parameterModel.getType()).thenReturn(toMetadataType(ValidModelUsingSdkApi.class));
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void collectionWithImplementingTypeUsingLegacyApi() {
    when(parameterModel.getType()).thenReturn(toMetadataType(CollectionWithDefaultImplementingTypeUsingLegacyApi.class));
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void dictionaryWithImplementingTypeUsingLegacyApi() {
    when(parameterModel.getType()).thenReturn(toMetadataType(DictionaryWithDefaultImplementingTypeUsingLegacyApi.class));
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void implementingTypeIsNotAssignableUsingLegacyApi() {
    when(parameterModel.getType()).thenReturn(toMetadataType(ImplementingTypeNotAssignableUsingLegacyApi.class));
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void basicTypeFieldUsingLegacyApi() {
    when(parameterModel.getType()).thenReturn(toMetadataType(BasicTypeWithNullSafeUsingLegacyApi.class));
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void abstractFieldTypeUsingLegacyApi() {
    when(parameterModel.getType()).thenReturn(toMetadataType(AbstractNullSafeTypeWithoutOverrideUsingLegacyApi.class));
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void abstractImplementingTypeUsingLegacyApi() {
    when(parameterModel.getType()).thenReturn(toMetadataType(AbstractNullSafeTypeWithAbstractOverrideUsingLegacyApi.class));
    validate(extensionModel, validator);
  }

  @Test
  public void validModeUsingLegacyApi() {
    when(parameterModel.getType()).thenReturn(toMetadataType(ValidModelUsingLegacyApi.class));
    validate(extensionModel, validator);
  }

  private static class CollectionWithDefaultImplementingTypeUsingSdkApi {

    @Parameter
    @org.mule.sdk.api.annotation.param.NullSafe(defaultImplementingType = LinkedList.class)
    @Optional
    private List<String> strings;
  }

  private static class DictionaryWithDefaultImplementingTypeUsingSdkApi {

    @Parameter
    @org.mule.sdk.api.annotation.param.NullSafe(defaultImplementingType = LinkedList.class)
    @Optional
    private Map<String, String> strings;
  }

  private static class BasicTypeWithNullSafeUsingSdkApi {

    @Parameter
    @org.mule.sdk.api.annotation.param.NullSafe
    @Optional
    private String bla;
  }

  private static class ImplementingTypeNotAssignableUsingSdkApi {

    @Parameter
    @org.mule.sdk.api.annotation.param.NullSafe(defaultImplementingType = UnrelatedPojo.class)
    @Optional
    private ParentPojo pojo;
  }

  private static class AbstractNullSafeTypeWithoutOverrideUsingSdkApi {

    @Parameter
    @org.mule.sdk.api.annotation.param.NullSafe
    @Optional
    private ParentPojo pojo;
  }

  private static class AbstractNullSafeTypeWithAbstractOverrideUsingSdkApi {

    @Parameter
    @org.mule.sdk.api.annotation.param.NullSafe(defaultImplementingType = AbstractChildPojo.class)
    @Optional
    private ParentPojo pojo;
  }

  private static class ValidModelUsingSdkApi {

    @Parameter
    @org.mule.sdk.api.annotation.param.NullSafe(defaultImplementingType = ChildPojo.class)
    @Optional
    private ParentPojo pojo;

    @Parameter
    @org.mule.sdk.api.annotation.param.NullSafe
    @Optional
    private ChildPojo childPojo;

    @Parameter
    @org.mule.sdk.api.annotation.param.NullSafe
    @Optional
    private Map<String, String> mapOfStrings;

    @Parameter
    @org.mule.sdk.api.annotation.param.NullSafe
    @Optional
    private List<String> listOfStrings;
  }

  private static class CollectionWithDefaultImplementingTypeUsingLegacyApi {

    @Parameter
    @NullSafe(defaultImplementingType = LinkedList.class)
    @Optional
    private List<String> strings;
  }

  private static class DictionaryWithDefaultImplementingTypeUsingLegacyApi {

    @Parameter
    @NullSafe(defaultImplementingType = LinkedList.class)
    @Optional
    private Map<String, String> strings;
  }

  private static class BasicTypeWithNullSafeUsingLegacyApi {

    @Parameter
    @NullSafe
    @Optional
    private String bla;
  }

  private static class ImplementingTypeNotAssignableUsingLegacyApi {

    @Parameter
    @NullSafe(defaultImplementingType = UnrelatedPojo.class)
    @Optional
    private ParentPojo pojo;
  }

  private static class AbstractNullSafeTypeWithoutOverrideUsingLegacyApi {

    @Parameter
    @NullSafe
    @Optional
    private ParentPojo pojo;
  }

  private static class AbstractNullSafeTypeWithAbstractOverrideUsingLegacyApi {

    @Parameter
    @NullSafe(defaultImplementingType = AbstractChildPojo.class)
    @Optional
    private ParentPojo pojo;
  }

  private static class ValidModelUsingLegacyApi {

    @Parameter
    @NullSafe(defaultImplementingType = ChildPojo.class)
    @Optional
    private ParentPojo pojo;

    @Parameter
    @NullSafe
    @Optional
    private ChildPojo childPojo;

    @Parameter
    @NullSafe
    @Optional
    private Map<String, String> mapOfStrings;

    @Parameter
    @NullSafe
    @Optional
    private List<String> listOfStrings;
  }

  private abstract static class ParentPojo {

  }

  private static class UnrelatedPojo {

  }

  private static class ChildPojo extends ParentPojo {

    public ChildPojo() {

    }
  }

  private abstract static class AbstractChildPojo extends ParentPojo {

  }
}
