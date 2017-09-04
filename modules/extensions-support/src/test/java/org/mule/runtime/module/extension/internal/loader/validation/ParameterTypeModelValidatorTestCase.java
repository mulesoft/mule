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
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ParameterTypeModelValidatorTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionModel extensionModel;

  @Mock
  private OperationModel operationModel;

  @Mock
  private ParameterModel parameter;

  private Type objectKey = new TypeToken<Map<Object, Object>>() {}.getType();
  private Type stringMap = new TypeToken<Map<String, Object>>() {}.getType();
  private Type wildcardMap = new TypeToken<Map<?, ?>>() {}.getType();
  private Type rawMap = new TypeToken<Map>() {}.getType();

  private ParameterTypeModelValidator validator = new ParameterTypeModelValidator();

  @Before
  public void before() {
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    when(parameter.getName()).thenReturn("parameter");
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
}
