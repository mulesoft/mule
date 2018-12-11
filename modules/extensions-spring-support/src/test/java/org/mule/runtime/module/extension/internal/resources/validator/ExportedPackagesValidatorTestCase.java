/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.validator;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.api.util.ExtensionModelTestUtils.visitableMock;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getApiMethods;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockParameters;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.OutputModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.extension.api.property.ClassLoaderModelProperty;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionOperationDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.OperationWrapper;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.test.metadata.extension.model.shapes.Shape;
import org.mule.test.module.extension.api.ApiTestClass;
import org.mule.test.module.extension.internal.InternalTestClass;
import org.mule.test.vegan.extension.VeganAttributes;

import com.google.common.reflect.TypeToken;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import org.hamcrest.collection.IsEmptyCollection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ExportedPackagesValidatorTestCase {

  private final ClassTypeLoader loader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  @Mock(lenient = true)
  private ExtensionModel extensionModel;

  @Before
  public void setup() {
    ClassLoaderModelProperty classLoaderModelProperty = new ClassLoaderModelProperty(getClass().getClassLoader());
    when(extensionModel.getModelProperty(ClassLoaderModelProperty.class)).thenReturn(of(classLoaderModelProperty));
  }

  @Test
  public void invalidExportedPackages() {
    setUpInvalidExtension();

    ExportedPackagesValidator exportedPackagesValidator = new ExportedPackagesValidator();
    ProblemsReporter problemsReporter = new ProblemsReporter(extensionModel);
    exportedPackagesValidator.validate(extensionModel, problemsReporter);

    List<Problem> errors = problemsReporter.getErrors();
    assertThat(errors, is(not(IsEmptyCollection.empty())));
    Problem error = errors.get(0);
    assertThat(error.getComponent(), is(extensionModel));
    assertThat(error.getMessage(), containsString("org.mule.test.module.extension.internal"));

    List<Problem> warnings = problemsReporter.getWarnings();
    assertThat(warnings, is(not(IsEmptyCollection.empty())));
    Problem warning = warnings.get(0);
    assertThat(warning.getComponent(), is(extensionModel));
    assertThat(warning.getMessage(), containsString("org.mule.test.metadata.extension.model.shapes"));
  }

  @Test
  public void validExportedPackages() {
    setUpValidExtension();
    ExportedPackagesValidator exportedPackagesValidator = new ExportedPackagesValidator();
    ProblemsReporter problemsReporter = new ProblemsReporter(extensionModel);
    exportedPackagesValidator.validate(extensionModel, problemsReporter);

    assertThat(problemsReporter.getErrors(), is(IsEmptyCollection.empty()));
    assertThat(problemsReporter.getWarnings(), is(IsEmptyCollection.empty()));
  }

  private void setUpInvalidExtension() {
    OutputModel appleList = mockOutputModel(InternalTestClass.class);
    OperationModel firstOperation = mockOperationModel(appleList, mockOutputModel(InternalTestClass.class));
    withMethod(firstOperation, getApiMethods(InternalTestClass.class).stream()
        .filter(m -> m.getName().equals("someOperation"))
        .findFirst());

    ParameterModel parameter = mockParameterModel(Shape.class);
    OutputModel resultList = mockOutputModel(new TypeToken<List<Result<Apple, VeganAttributes>>>() {}.getType());
    OperationModel secondOperation = mockOperationModel(resultList, mockOutputModel(List.class), parameter);
    withMethod(secondOperation, empty());

    when(extensionModel.getOperationModels()).thenReturn(asList(firstOperation, secondOperation));
    visitableMock(firstOperation, secondOperation);
  }

  private void setUpValidExtension() {
    OutputModel appleList = mockOutputModel(ApiTestClass.class);
    OperationModel firstOperation = mockOperationModel(appleList, mockOutputModel(ApiTestClass.class));
    withMethod(firstOperation, getApiMethods(ApiTestClass.class).stream()
        .filter(m -> m.getName().equals("someOperation"))
        .findFirst());

    when(extensionModel.getOperationModels()).thenReturn(asList(firstOperation));
    visitableMock(firstOperation);
  }

  private void withMethod(OperationModel operationModel, Optional<Method> optionalMethod) {
    when(operationModel.getModelProperty(ImplementingMethodModelProperty.class))
        .thenReturn(optionalMethod.map(ImplementingMethodModelProperty::new));
    when(operationModel.getModelProperty(ExtensionOperationDescriptorModelProperty.class))
        .thenReturn(optionalMethod
            .map(method -> new ExtensionOperationDescriptorModelProperty(new OperationWrapper(method, loader))));
  }

  private OutputModel mockOutputModel(Type type) {
    OutputModel om = mock(OutputModel.class, withSettings().lenient());
    when(om.getType()).thenReturn(loader.load(type));
    return om;
  }

  private ParameterModel mockParameterModel(Type type) {
    ParameterModel pm = mock(ParameterModel.class);
    when(pm.getType()).thenReturn(loader.load(type));
    return pm;
  }

  private OperationModel mockOperationModel(OutputModel output, OutputModel attributes, ParameterModel... params) {
    OperationModel op = mock(OperationModel.class, withSettings().lenient());
    when(op.getOutput()).thenReturn(output);
    when(op.getOutputAttributes()).thenReturn(attributes);
    if (params != null) {
      mockParameters(op, params);
    }
    return op;
  }
}
