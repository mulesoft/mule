/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.validate;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.java.api.JavaTypeLoader;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.loader.java.property.CompileTimeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;
import org.mule.tck.size.SmallTest;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ParameterGroupModelValidatorTestCase {

  @Mock
  private ExtensionModel extensionModel;

  @Mock
  private OperationModel operationModel;

  @Mock
  private ParameterGroupModel groupModel;

  @Mock
  private ParameterModel parameterModel;

  private ClassTypeLoader typeLoader = new JavaTypeLoader(Thread.currentThread().getContextClassLoader());

  private ParameterGroupModelValidator validator = new ParameterGroupModelValidator();

  @Before
  public void before() {
    when(extensionModel.getModelProperty(CompileTimeModelProperty.class)).thenReturn(of(mock(CompileTimeModelProperty.class)));
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    when(operationModel.getParameterGroupModels()).thenReturn(asList(groupModel));
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void invalidModelDueToNonInstantiableParameterGroup() {
    TypeWrapper type = new TypeWrapper(Serializable.class, typeLoader);
    ParameterGroupDescriptor descriptor =
        new ParameterGroupDescriptor("name", type, null, mock(AnnotatedElement.class), null);

    when(groupModel.getModelProperty(ParameterGroupModelProperty.class))
        .thenReturn(of(new ParameterGroupModelProperty(descriptor)));

    when(groupModel.getParameterModels()).thenReturn(asList(parameterModel));
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void invalidModelDueEmptyParameterGroup() {
    ParameterGroupDescriptor descriptor =
        new ParameterGroupDescriptor("name", new TypeWrapper(EmptyGroupPojo.class, typeLoader),
                                     null, mock(AnnotatedElement.class), null);
    when(groupModel.getModelProperty(ParameterGroupModelProperty.class))
        .thenReturn(of(new ParameterGroupModelProperty(descriptor)));

    validate(extensionModel, validator);
  }

  @Test
  public void skipEmptyGroupValidationInRuntimeMode() {
    when(extensionModel.getModelProperty(CompileTimeModelProperty.class)).thenReturn(empty());
    ParameterGroupDescriptor descriptor =
        new ParameterGroupDescriptor("name", new TypeWrapper(EmptyGroupPojo.class, typeLoader),
                                     null, mock(AnnotatedElement.class), null);
    when(groupModel.getModelProperty(ParameterGroupModelProperty.class))
        .thenReturn(of(new ParameterGroupModelProperty(descriptor)));

    validate(extensionModel, validator);
  }

  public static class EmptyGroupPojo {

  }
}
