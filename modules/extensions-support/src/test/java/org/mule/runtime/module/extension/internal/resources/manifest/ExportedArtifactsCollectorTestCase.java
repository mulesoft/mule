/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.manifest;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.util.ExtensionModelTestUtils.visitableMock;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getApiMethods;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockParameters;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.OutputModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.property.ClassLoaderModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.test.heisenberg.extension.HeisenbergOperations;
import org.mule.test.metadata.extension.model.shapes.Shape;
import org.mule.test.vegan.extension.VeganAttributes;

import com.google.common.reflect.TypeToken;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ExportedArtifactsCollectorTestCase {

  private final ClassTypeLoader loader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  private static final String APPLE_PACKAGE = "org.mule.tck.testmodels.fruit";
  private static final String PEEL_PACKAGE = "org.mule.tck.testmodels.fruit.peel";
  private static final String SEED_PACKAGE = "org.mule.tck.testmodels.fruit.seed";
  private static final String VEGAN_PACKAGE = "org.mule.test.vegan.extension";
  private static final String SHAPE_PACKAGE = "org.mule.test.metadata.extension.model.shapes";
  private static final String EXCEPTION_PACKAGE = "org.mule.test.heisenberg.extension.exception";

  @Mock
  private ExtensionModel extensionModel;

  private ExportedArtifactsCollector collector;

  @Before
  public void setup() {
    ClassLoaderModelProperty classLoaderModelProperty = new ClassLoaderModelProperty(getClass().getClassLoader());
    when(extensionModel.getModelProperty(ClassLoaderModelProperty.class)).thenReturn(of(classLoaderModelProperty));

    OutputModel appleList = mockOutputModel(new TypeToken<List<Apple>>() {}.getType());
    OperationModel firstOperation = mockOperationModel(appleList, mockOutputModel(List.class));
    withMethod(firstOperation, getApiMethods(HeisenbergOperations.class).stream()
        .filter(m -> m.getName().equals("callGusFring"))
        .findFirst());

    ParameterModel parameter = mockParameterModel(Shape.class);
    OutputModel resultList = mockOutputModel(new TypeToken<List<Result<Apple, VeganAttributes>>>() {}.getType());
    OperationModel secondOperation = mockOperationModel(resultList, mockOutputModel(List.class), parameter);
    withMethod(secondOperation, empty());
    when(extensionModel.getOperationModels()).thenReturn(asList(firstOperation, secondOperation));
    visitableMock(firstOperation, secondOperation);
    collector = new ExportedArtifactsCollector(extensionModel);
  }

  private void withMethod(OperationModel operationModel, Optional<Method> method) {
    when(operationModel.getModelProperty(ImplementingMethodModelProperty.class))
        .thenReturn(method.map(ImplementingMethodModelProperty::new));
  }

  @Test
  public void collect() {
    Set<String> exportedPackages = collector.getExportedPackages();
    assertThat(exportedPackages, hasSize(6));
    assertThat(exportedPackages,
               containsInAnyOrder(SHAPE_PACKAGE, APPLE_PACKAGE, VEGAN_PACKAGE, PEEL_PACKAGE, SEED_PACKAGE, EXCEPTION_PACKAGE));
  }

  private OutputModel mockOutputModel(Type type) {
    OutputModel om = mock(OutputModel.class);
    when(om.getType()).thenReturn(loader.load(type));
    return om;
  }

  private ParameterModel mockParameterModel(Type type) {
    ParameterModel pm = mock(ParameterModel.class);
    when(pm.getType()).thenReturn(loader.load(type));
    return pm;
  }

  private OperationModel mockOperationModel(OutputModel output, OutputModel attributes, ParameterModel... params) {
    OperationModel op = mock(OperationModel.class);
    when(op.getOutput()).thenReturn(output);
    when(op.getOutputAttributes()).thenReturn(attributes);
    if (params != null) {
      mockParameters(op, params);
    }
    return op;
  }
}
