/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.OutputModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
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
  private ParameterModel invalidParameterModel;

  @Mock
  private OutputModel outputModel;

  private ExportedTypesModelValidator validator = new ExportedTypesModelValidator();

  @Before
  public void before() {
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    when(extensionModel.getSourceModels()).thenReturn(asList(sourceModel));
    when(operationModel.getName()).thenReturn("dummyOperation");
    when(operationModel.getOutput()).thenReturn(outputModel);
    when(sourceModel.getOutput()).thenReturn(outputModel);
    when(outputModel.getType()).thenReturn(toMetadataType(void.class));
    when(sourceModel.getName()).thenReturn("dummySource");
  }

  @Test(expected = IllegalParameterModelDefinitionException.class)
  public void invalidModelDueToOperationWithArgumentParameterWithoutGetter() {
    when(invalidParameterModel.getType()).thenReturn(toMetadataType(PojoWithParameterWithoutGetter.class));
    when(invalidParameterModel.getName()).thenReturn("pojo");
    when(operationModel.getAllParameterModels()).thenReturn(asList(invalidParameterModel));
    validator.validate(extensionModel);
  }

  @Test(expected = IllegalParameterModelDefinitionException.class)
  public void invalidModelDueToOperationWithReturnTypeParameterWithoutGetter() {
    when(operationModel.getAllParameterModels()).thenReturn(emptyList());
    when(operationModel.getOutput()).thenReturn(outputModel);
    when(outputModel.getType()).thenReturn(toMetadataType(PojoWithParameterWithoutGetter.class));
    validator.validate(extensionModel);
  }

  @Test(expected = IllegalParameterModelDefinitionException.class)
  public void invalidModelDueToSourceWithReturnTypeParameterWithoutGetter() {
    when(sourceModel.getAllParameterModels()).thenReturn(emptyList());
    when(sourceModel.getOutput()).thenReturn(outputModel);
    when(outputModel.getType()).thenReturn(toMetadataType(PojoWithParameterWithoutGetter.class));
    validator.validate(extensionModel);
  }

  @Test
  public void outputAttributesAreNotValidated() {
    OutputModel outputAttributes = mock(OutputModel.class);
    when(sourceModel.getAllParameterModels()).thenReturn(emptyList());
    when(sourceModel.getOutput()).thenReturn(outputModel);
    when(sourceModel.getOutputAttributes()).thenReturn(outputAttributes);
    when(outputModel.getType()).thenReturn(toMetadataType(PojoWithParameterWithGetter.class));
    when(outputAttributes.getType()).thenReturn(toMetadataType(PojoWithParameterWithoutGetter.class));
    validator.validate(extensionModel);
  }


  private static class PojoWithParameterWithoutGetter {

    @Parameter
    private String fieldWithoutGetter;
  }

  private static class PojoWithParameterWithGetter {

    @Parameter
    private String fieldWithGetter;

    public String getFieldWithGetter() {
      return fieldWithGetter;
    }
  }

}
