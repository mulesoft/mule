/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.util.Arrays.asList;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockParameters;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.validate;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.java.property.CompileTimeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionParameterDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class HasDefaultConstructorModelValidatorTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock(answer = RETURNS_DEEP_STUBS, lenient = true)
  private ExtensionModel extensionModel;

  @Mock(lenient = true)
  private OperationModel operationModel;

  @Mock(lenient = true)
  private ParameterModel parameter;

  @Mock
  private ExtensionParameter extensionParameter;

  private HasDefaultConstructorModelValidator validator = new HasDefaultConstructorModelValidator();

  @Before
  public void before() {
    when(extensionModel.getModelProperty(CompileTimeModelProperty.class)).thenReturn(Optional.of(new CompileTimeModelProperty()));
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));

    when(parameter.getModelProperty(ExtensionParameterDescriptorModelProperty.class))
        .thenReturn(Optional.of(new ExtensionParameterDescriptorModelProperty(extensionParameter)));

    mockParameters(operationModel, parameter);
  }

  @Test
  public void pojoRequiresDefaultConstructor() {
    expectedException.expect(IllegalModelDefinitionException.class);
    expectedException
        .expectMessage("Type 'InvalidPojoRequiresDefaultConstructor' does not have a default constructor");
    when(parameter.getType()).thenReturn(TYPE_LOADER.load(InvalidPojoRequiresDefaultConstructor.class));
    when(extensionParameter.getType()).thenReturn(new TypeWrapper(InvalidPojoRequiresDefaultConstructor.class, TYPE_LOADER));
    validate(extensionModel, validator);

  }

  @Test
  public void pojoWithImplicitDefaultConstructor() {
    when(parameter.getType()).thenReturn(TYPE_LOADER.load(PojoWithImplicitDefaultConstructor.class));
    when(extensionParameter.getType()).thenReturn(new TypeWrapper(PojoWithImplicitDefaultConstructor.class, TYPE_LOADER));
    validate(extensionModel, validator);

  }

  @Test
  public void pojoWithDeclaredDefaultConstructor() {
    when(parameter.getType()).thenReturn(TYPE_LOADER.load(PojoWithDefaultConstructor.class));
    when(extensionParameter.getType()).thenReturn(new TypeWrapper(PojoWithDefaultConstructor.class, TYPE_LOADER));
    validate(extensionModel, validator);

  }



  public static class InvalidPojoRequiresDefaultConstructor {

    @Parameter
    private String bar;

    @Parameter
    private String id;

    public InvalidPojoRequiresDefaultConstructor(String bar, String id) {
      this.bar = bar;
      this.id = id;
    }
  }

  public static class PojoWithImplicitDefaultConstructor {

  }

  public static class PojoWithDefaultConstructor {

    public PojoWithDefaultConstructor() {}
  }
}
