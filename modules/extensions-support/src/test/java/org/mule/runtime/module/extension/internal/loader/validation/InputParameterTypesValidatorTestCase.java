/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Optional.of;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.util.ExtensionModelTestUtils.visitableMock;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.validate;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.OutputModel;
import org.mule.runtime.api.meta.model.SubTypesModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceCallbackModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import com.google.common.collect.ImmutableSet;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class InputParameterTypesValidatorTestCase extends AbstractMuleTestCase {

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionModel extensionModel;

  @Mock
  private OperationModel operationModel;

  @Mock
  private SourceModel sourceModel;

  @Mock
  private SourceCallbackModel sourceCallbackModel;

  @Mock
  private ParameterModel invalidParameterModel;

  @Mock
  private OutputModel outputModel;

  private InputParametersTypeModelValidator validator = new InputParametersTypeModelValidator();

  @Before
  public void before() {
    when(extensionModel.getName()).thenReturn("dummyExtension");
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    when(extensionModel.getSourceModels()).thenReturn(asList(sourceModel));
    when(extensionModel.getSubTypes()).thenReturn(emptySet());
    when(operationModel.getName()).thenReturn("dummyOperation");
    when(operationModel.getOutput()).thenReturn(outputModel);
    when(sourceModel.getOutput()).thenReturn(outputModel);
    when(outputModel.getType()).thenReturn(toMetadataType(void.class));
    when(sourceModel.getName()).thenReturn("dummySource");
    when(sourceModel.getErrorCallback()).thenReturn(of(sourceCallbackModel));
    when(sourceModel.getSuccessCallback()).thenReturn(of(sourceCallbackModel));
    when(sourceModel.getTerminateCallback()).thenReturn(of(sourceCallbackModel));
    visitableMock(operationModel, sourceModel);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void invalidModelDueToOperationWithArgumentParameterWithoutGetter() {
    validateOperationParameterOfType(toMetadataType(PojoWithParameterWithoutGetter.class));
  }

  @Test
  public void validModelDueToOperationWithArgumentParameterWithGetter() {
    validateOperationParameterOfType(toMetadataType(PojoWithParameterWithGetter.class));
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void invalidModelDueToOperationArgumentWithSubtypeWithoutGetter() {
    validateOperationParameterOfType(toMetadataType(BaseType.class));
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void invalidModelDueToOperationArgumentWithListOfSubtypeWithoutGetter() {
    validateOperationParameterOfType(TYPE_LOADER.load(new com.google.common.reflect.TypeToken<List<BaseType>>() {}.getType()));
  }

  private void validateOperationParameterOfType(MetadataType operationParameterType) {
    when(invalidParameterModel.getType()).thenReturn(operationParameterType);
    when(invalidParameterModel.getName()).thenReturn("pojos");

    when(operationModel.getAllParameterModels()).thenReturn(asList(invalidParameterModel));
    SubTypesModel subTypesModel = new SubTypesModel(toMetadataType(BaseType.class), ImmutableSet
        .of(toMetadataType(PojoWithParameterWithoutGetter.class), toMetadataType(PojoWithParameterWithGetter.class)));
    when(extensionModel.getSubTypes()).thenReturn(ImmutableSet.of(subTypesModel));
    validate(extensionModel, validator);
  }

  @Test
  public void validModelDueSubtypeWithoutGetterNotUsedAsInputParameter() {
    SubTypesModel subTypesModel =
        new SubTypesModel(toMetadataType(BaseType.class), ImmutableSet.of(toMetadataType(PojoWithParameterWithoutGetter.class)));
    when(extensionModel.getSubTypes()).thenReturn(ImmutableSet.of(subTypesModel));
    when(extensionModel.getOperationModels()).thenReturn(emptyList());
    validate(extensionModel, validator);
  }

  @Test
  public void operationOutputIsNotValidated() {
    when(operationModel.getAllParameterModels()).thenReturn(emptyList());
    when(operationModel.getOutput()).thenReturn(outputModel);
    when(outputModel.getType()).thenReturn(toMetadataType(PojoWithParameterWithoutGetter.class));
    validate(extensionModel, validator);
  }

  @Test
  public void sourceOutputIsNotValidated() {
    when(sourceModel.getAllParameterModels()).thenReturn(emptyList());
    when(sourceModel.getOutput()).thenReturn(outputModel);
    when(outputModel.getType()).thenReturn(toMetadataType(PojoWithParameterWithoutGetter.class));
    validate(extensionModel, validator);
  }

  @Test
  public void operationAttributesAreNotValidated() {
    when(operationModel.getAllParameterModels()).thenReturn(emptyList());
    when(outputModel.getType()).thenReturn(toMetadataType(PojoWithParameterWithoutGetter.class));
    when(operationModel.getOutput()).thenReturn(outputModel);
    when(operationModel.getOutputAttributes()).thenReturn(outputModel);
  }

  private static class PojoWithParameterWithoutGetter extends BaseType {

    @Parameter
    private String fieldWithoutGetter;
  }


  private static class PojoWithParameterWithGetter extends BaseType {

    @Parameter
    private String fieldWithGetter;

    public String getFieldWithGetter() {
      return fieldWithGetter;
    }
  }

  private static class BaseType {

    @Parameter
    private String baseFieldWithGetter;

    public String getBaseFieldWithGetter() {
      return baseFieldWithGetter;
    }
  }

}
