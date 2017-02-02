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
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class NullSafeModelValidatorTestCase extends AbstractMuleTestCase {

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionModel extensionModel;

  @Mock
  private OperationModel operationModel;

  @Mock
  private ParameterModel parameterModel;

  private ExtensionModelValidator validator = new NullSafeModelValidator();

  @Before
  public void before() {
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    mockParameters(operationModel, parameterModel);
    when(extensionModel.getImportedTypes()).thenReturn(emptySet());
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void collectionWithImplementingType() {
    when(parameterModel.getType()).thenReturn(toMetadataType(CollectionWithDefaultImplementingType.class));
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void dictionaryWithImplementingType() {
    when(parameterModel.getType()).thenReturn(toMetadataType(DictionaryWithDefaultImplementingType.class));
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void implementingTypeIsNotAssignable() {
    when(parameterModel.getType()).thenReturn(toMetadataType(ImplementingTypeNotAssignable.class));
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void basicTypeField() {
    when(parameterModel.getType()).thenReturn(toMetadataType(BasicTypeWithNullSafe.class));
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void abstractFieldType() {
    when(parameterModel.getType()).thenReturn(toMetadataType(AbstractNullSafeTypeWithoutOverride.class));
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void abstractImplementingType() {
    when(parameterModel.getType()).thenReturn(toMetadataType(AbstractNullSafeTypeWithAbstractOverride.class));
    validate(extensionModel, validator);
  }

  @Test
  public void validModel() {
    when(parameterModel.getType()).thenReturn(toMetadataType(ValidModel.class));
    validate(extensionModel, validator);
  }

  private static class CollectionWithDefaultImplementingType {

    @Parameter
    @NullSafe(defaultImplementingType = LinkedList.class)
    @Optional
    private List<String> strings;
  }

  private static class DictionaryWithDefaultImplementingType {

    @Parameter
    @NullSafe(defaultImplementingType = LinkedList.class)
    @Optional
    private Map<String, String> strings;
  }

  private static class BasicTypeWithNullSafe {

    @Parameter
    @NullSafe
    @Optional
    private String bla;
  }

  private static class ImplementingTypeNotAssignable {

    @Parameter
    @NullSafe(defaultImplementingType = UnrelatedPojo.class)
    @Optional
    private ParentPojo pojo;
  }

  private static class AbstractNullSafeTypeWithoutOverride {

    @Parameter
    @NullSafe
    @Optional
    private ParentPojo pojo;
  }

  private static class AbstractNullSafeTypeWithAbstractOverride {

    @Parameter
    @NullSafe(defaultImplementingType = AbstractChildPojo.class)
    @Optional
    private ParentPojo pojo;
  }

  private static class ValidModel {

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
