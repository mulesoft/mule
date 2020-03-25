/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.util.Arrays.asList;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockParameters;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.validate;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.extension.api.annotation.param.ConfigOverride;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.stereotype.ComponentId;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.java.property.CompileTimeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionParameterDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.gson.reflect.TypeToken;

@SmallTest
public class ParameterTypeModelValidatorTestCase extends AbstractMuleTestCase {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Rule
  public ExpectedException expectedException = none();

  @Mock(answer = RETURNS_DEEP_STUBS, lenient = true)
  private ExtensionModel extensionModel;

  @Mock(lenient = true)
  private OperationModel operationModel;

  @Mock(lenient = true)
  private ParameterModel parameter;

  @Mock
  private ExtensionParameter extensionParameter;


  private final Type objectKey = new TypeToken<Map<Object, Object>>() {}.getType();
  private final Type stringMap = new TypeToken<Map<String, Object>>() {}.getType();
  private final Type wildcardMap = new TypeToken<Map<?, ?>>() {}.getType();
  private final Type rawMap = new TypeToken<Map>() {}.getType();

  private final ParameterTypeModelValidator validator = new ParameterTypeModelValidator();

  @Before
  public void before() {
    when(extensionModel.getModelProperty(CompileTimeModelProperty.class)).thenReturn(Optional.of(new CompileTimeModelProperty()));
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));

    when(parameter.getName()).thenReturn("parameter");
    when(parameter.getModelProperty(ExtensionParameterDescriptorModelProperty.class))
        .thenReturn(Optional.of(new ExtensionParameterDescriptorModelProperty(extensionParameter)));


    mockParameters(operationModel, parameter);
  }

  @Test
  public void objectKey() {
    expectedException.expect(IllegalModelDefinitionException.class);
    when(parameter.getType()).thenReturn(TYPE_LOADER.load(objectKey));
    validate(extensionModel, validator);
  }

  @Test
  public void stringKey() {
    when(parameter.getType()).thenReturn(TYPE_LOADER.load(stringMap));
    validate(extensionModel, validator);
  }

  @Test
  public void wildcardMap() {
    expectedException.expect(IllegalModelDefinitionException.class);
    when(parameter.getType()).thenReturn(TYPE_LOADER.load(wildcardMap));
    validate(extensionModel, validator);
  }

  @Test
  public void rawMap() {
    when(parameter.getType()).thenReturn(TYPE_LOADER.load(rawMap));
    validate(extensionModel, validator);
  }

  @Test
  public void boxedBoolean() {
    expectedException.expect(IllegalModelDefinitionException.class);
    when(parameter.getType()).thenReturn(TYPE_LOADER.load(Boolean.class));
    validate(extensionModel, validator);
  }

  @Test
  public void primitiveBoolean() {
    when(parameter.getType()).thenReturn(TYPE_LOADER.load(boolean.class));
    validate(extensionModel, validator);
  }

  @Test
  public void configOverrideInPojo() {
    expectedException.expect(IllegalModelDefinitionException.class);
    expectedException
        .expectMessage("Type 'InvalidPojoWithConfigOverride' has a field with name 'overriden' declared as 'ConfigOverride', which is not allowed.");
    when(parameter.getType()).thenReturn(TYPE_LOADER.load(InvalidPojoWithConfigOverride.class));
    when(extensionParameter.getType()).thenReturn(new TypeWrapper(InvalidPojoWithConfigOverride.class, TYPE_LOADER));
    validate(extensionModel, validator);
  }

  @Test
  public void componentIdInPojo() {
    expectedException.expect(IllegalModelDefinitionException.class);
    expectedException
        .expectMessage("Type 'InvalidPojoWithComponentId' has a field with name 'id' declared as 'ComponentId', which is not allowed.");
    when(parameter.getType()).thenReturn(TYPE_LOADER.load(InvalidPojoWithComponentId.class));
    when(extensionParameter.getType()).thenReturn(new TypeWrapper(InvalidPojoWithComponentId.class, TYPE_LOADER));
    validate(extensionModel, validator);
  }

  private static class InvalidPojoWithConfigOverride {

    @Parameter
    private String foo;

    @Parameter
    @ConfigOverride
    private String overriden;
  }

  private static class InvalidPojoWithComponentId {

    @Parameter
    private String bar;

    @Parameter
    @ComponentId
    private String id;
  }
}
