/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.validate;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.OutputModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceCallbackModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ExportedTypesValidatorTestCase extends AbstractMuleTestCase {

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

  private ExportedTypesModelValidator validator = new ExportedTypesModelValidator();

  @Before
  public void before() {
    when(invalidParameterModel.getType()).thenReturn(toMetadataType(PojoWithParameterWithoutGetter.class));
    when(invalidParameterModel.getName()).thenReturn("pojo");

    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    when(extensionModel.getSourceModels()).thenReturn(asList(sourceModel));
    when(operationModel.getName()).thenReturn("dummyOperation");
    when(operationModel.getOutput()).thenReturn(outputModel);
    when(sourceModel.getOutput()).thenReturn(outputModel);
    when(outputModel.getType()).thenReturn(toMetadataType(void.class));
    when(sourceModel.getName()).thenReturn("dummySource");
    when(sourceModel.getErrorCallback()).thenReturn(of(sourceCallbackModel));
    when(sourceModel.getSuccessCallback()).thenReturn(of(sourceCallbackModel));
    when(sourceCallbackModel.getAllParameterModels()).thenReturn(emptyList());
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void invalidModelDueToOperationWithArgumentParameterWithoutGetter() {
    when(operationModel.getAllParameterModels()).thenReturn(asList(invalidParameterModel));
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void invalidModelDueToSourceCallbackWithoutGetter() {
    when(sourceCallbackModel.getAllParameterModels()).thenReturn(asList(invalidParameterModel));
    validate(extensionModel, validator);
  }

  @Test
  public void sourceParameterAreNotValidated() {
    when(sourceModel.getAllParameterModels()).thenReturn(asList(invalidParameterModel));
    validate(extensionModel, validator);
  }

  @Test
  public void sourceOutputAttributesAreNotValidated() {
    OutputModel outputAttributes = mock(OutputModel.class);
    when(sourceModel.getAllParameterModels()).thenReturn(emptyList());
    when(sourceModel.getOutput()).thenReturn(outputModel);
    when(sourceModel.getOutputAttributes()).thenReturn(outputAttributes);
    when(outputModel.getType()).thenReturn(toMetadataType(PojoWithParameterWithGetterAndSetter.class));
    when(outputAttributes.getType()).thenReturn(toMetadataType(PojoWithParameterWithoutGetter.class));
    validate(extensionModel, validator);
  }

  @Test
  public void sourceOutputTypeIsNotValidated() {
    OutputModel outputWithoutGetters = mock(OutputModel.class);
    when(outputWithoutGetters.getType()).thenReturn(toMetadataType(PojoWithParameterWithoutGetter.class));
    when(sourceModel.getAllParameterModels()).thenReturn(emptyList());
    when(sourceModel.getOutput()).thenReturn(outputWithoutGetters);
    when(sourceModel.getOutputAttributes()).thenReturn(outputModel);
    validate(extensionModel, validator);
  }

  @Test
  public void operationOutputAttributesAreNotValidated() {
    OutputModel outputAttributes = mock(OutputModel.class);
    when(operationModel.getAllParameterModels()).thenReturn(emptyList());
    when(operationModel.getOutput()).thenReturn(outputModel);
    when(operationModel.getOutputAttributes()).thenReturn(outputAttributes);
    when(outputModel.getType()).thenReturn(toMetadataType(PojoWithParameterWithGetterAndSetter.class));
    when(outputAttributes.getType()).thenReturn(toMetadataType(PojoWithParameterWithoutGetter.class));
    validate(extensionModel, validator);
  }

  @Test
  public void operationOutputTypeIsNotValidated() {
    OutputModel outputWithoutGetters = mock(OutputModel.class);
    when(outputWithoutGetters.getType()).thenReturn(toMetadataType(PojoWithParameterWithoutGetter.class));
    when(operationModel.getAllParameterModels()).thenReturn(emptyList());
    when(operationModel.getOutput()).thenReturn(outputWithoutGetters);
    when(operationModel.getOutputAttributes()).thenReturn(outputModel);
    validate(extensionModel, validator);
  }


  private static class PojoWithParameterWithoutGetter {

    @Parameter
    private String fieldWithoutGetter;
  }


  private static class PojoWithParameterWithGetterAndSetter {

    @Parameter
    private String field;

    public String getField() {
      return field;
    }

    public void setField(String field) {
      this.field = field;
    }
  }

}
