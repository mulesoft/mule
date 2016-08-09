/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.util.Arrays.asList;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.arrayOf;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.dictionaryOf;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.objectTypeBuilder;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getField;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.introspection.property.ImportedTypesModelProperty;
import org.mule.runtime.extension.api.introspection.property.SubTypesModelProperty;
import org.mule.runtime.module.extension.internal.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.module.extension.internal.introspection.ParameterGroup;
import org.mule.runtime.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ParameterModelValidatorTestCase extends AbstractMuleTestCase {

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionModel extensionModel;

  @Mock
  private OperationModel operationModel;

  @Mock
  private ParameterModel validParameterModel;

  @Mock
  private ParameterModel invalidParameterModel;

  private ParameterModelValidator validator = new ParameterModelValidator();

  @Before
  public void before() {
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    when(extensionModel.getModelProperty(SubTypesModelProperty.class)).thenReturn(Optional.empty());
    when(extensionModel.getModelProperty(ImportedTypesModelProperty.class)).thenReturn(Optional.empty());
    when(validParameterModel.getModelProperty(ParameterGroupModelProperty.class)).thenReturn(Optional.empty());
    when(invalidParameterModel.getModelProperty(ParameterGroupModelProperty.class)).thenReturn(Optional.empty());
    when(operationModel.getName()).thenReturn("dummyOperation");
    when(extensionModel.getName()).thenReturn("extensionModel");
  }

  @Test
  public void validModel() {
    when(validParameterModel.getType()).thenReturn(toMetadataType(String.class));
    when(validParameterModel.getName()).thenReturn("url");
    when(operationModel.getParameterModels()).thenReturn(asList(validParameterModel));

    validator.validate(extensionModel);
  }

  @Test(expected = IllegalParameterModelDefinitionException.class)
  public void invalidModelDueToReservedName() {
    when(invalidParameterModel.getType()).thenReturn(toMetadataType(String.class));
    when(invalidParameterModel.getName()).thenReturn("name");
    when(operationModel.getParameterModels()).thenReturn(asList(invalidParameterModel));
    validator.validate(extensionModel);
  }

  @Test(expected = IllegalParameterModelDefinitionException.class)
  public void invalidParameterDueToReservedName() {
    when(invalidParameterModel.getType()).thenReturn(toMetadataType(InvalidPojo.class));
    when(invalidParameterModel.getName()).thenReturn("pojo");
    when(operationModel.getParameterModels()).thenReturn(asList(invalidParameterModel));
    validator.validate(extensionModel);
  }

  @Test(expected = IllegalParameterModelDefinitionException.class)
  public void invalidParameterCollectionDueToReservedName() {
    when(invalidParameterModel.getType())
        .thenReturn(ExtensionsTestUtils.arrayOf(List.class, objectTypeBuilder(InvalidPojo.class)));
    when(invalidParameterModel.getType()).thenReturn(arrayOf(List.class, objectTypeBuilder(InvalidPojo.class)));
    when(invalidParameterModel.getName()).thenReturn("pojos");
    when(operationModel.getParameterModels()).thenReturn(asList(invalidParameterModel));
    validator.validate(extensionModel);
  }

  @Test(expected = IllegalParameterModelDefinitionException.class)
  public void invalidParameterDictionaryDueToReservedName() {
    when(invalidParameterModel.getType())
        .thenReturn(dictionaryOf(Map.class, objectTypeBuilder(String.class), objectTypeBuilder(InvalidPojo.class)));
    when(invalidParameterModel.getName()).thenReturn("pojos");
    when(operationModel.getParameterModels()).thenReturn(asList(invalidParameterModel));
    validator.validate(extensionModel);
  }

  @Test(expected = IllegalParameterModelDefinitionException.class)
  public void invalidRecursiveParameterDueToReservedName() {
    when(invalidParameterModel.getType()).thenReturn(toMetadataType(RecursivePojo.class));
    when(invalidParameterModel.getName()).thenReturn("pojo");
    when(operationModel.getParameterModels()).thenReturn(asList(invalidParameterModel));
    validator.validate(extensionModel);
  }

  @Test(expected = IllegalParameterModelDefinitionException.class)
  public void invalidNestedParameterDueToReservedName() {
    when(invalidParameterModel.getType()).thenReturn(toMetadataType(NestedInvalidPojo.class));
    when(invalidParameterModel.getName()).thenReturn("pojo");
    when(operationModel.getParameterModels()).thenReturn(asList(invalidParameterModel));
    validator.validate(extensionModel);
  }

  @Test(expected = IllegalParameterModelDefinitionException.class)
  public void invalidModelDueToDefaultValueWhenRequired() {
    when(invalidParameterModel.getType()).thenReturn(toMetadataType(String.class));
    when(invalidParameterModel.isRequired()).thenReturn(true);
    when(invalidParameterModel.getName()).thenReturn("url");
    when(invalidParameterModel.getDefaultValue()).thenReturn("default");
    when(operationModel.getParameterModels()).thenReturn(asList(invalidParameterModel));
    validator.validate(extensionModel);
  }

  @Test(expected = IllegalParameterModelDefinitionException.class)
  public void invalidModelDueToNoReturnType() {
    when(invalidParameterModel.getType()).thenReturn(null);
    when(invalidParameterModel.getName()).thenReturn("url");
    when(operationModel.getParameterModels()).thenReturn(asList(invalidParameterModel));
    validator.validate(extensionModel);
  }

  @Test(expected = IllegalParameterModelDefinitionException.class)
  public void invalidModelDueToNonInstantiableParameterGroup() {
    ParameterGroup child =
        new ParameterGroup(Serializable.class, getField(InvalidPojoParameterGroup.class, "nonInstantiableField"));
    when(invalidParameterModel.getModelProperty(ParameterGroupModelProperty.class))
        .thenReturn(Optional.of(new ParameterGroupModelProperty(asList(child))));
    when(invalidParameterModel.getType()).thenReturn(toMetadataType(Serializable.class));
    when(invalidParameterModel.getName()).thenReturn("nonInstantiableField");
    when(operationModel.getParameterModels()).thenReturn(asList(invalidParameterModel));
    validator.validate(extensionModel);
  }

  private static class InvalidPojo {

    public InvalidPojo() {
      // needs to be instantiable
    }

    @Parameter
    private String name;
  }

  public static class NestedInvalidPojo {

    @Parameter
    private InvalidPojo invalidPojo;
  }

  public static class RecursivePojo {

    @Parameter
    private RecursivePojo pojo;

    @Parameter
    private InvalidPojo invalidPojo;
  }

  public static class InvalidPojoParameterGroup {

    @org.mule.runtime.extension.api.annotation.ParameterGroup
    private Serializable nonInstantiableField;
  }

}
