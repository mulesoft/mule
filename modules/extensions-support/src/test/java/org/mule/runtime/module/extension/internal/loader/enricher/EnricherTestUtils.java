/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.NamedDeclaration;
import org.mule.runtime.api.meta.model.display.LayoutModel;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

class EnricherTestUtils {

  private EnricherTestUtils() {}

  public static <T extends NamedDeclaration> T getDeclaration(List<T> operationList, String name) {
    return operationList.stream().filter(operation -> operation.getName().equals(name)).collect(toList()).get(0);
  }

  static <T extends NamedObject> T getNamedObject(List<T> operationList, String name) {
    return operationList.stream().filter(operation -> operation.getName().equals(name)).collect(toList()).get(0);
  }

  static <T extends ModelProperty> T checkIsPresent(BaseDeclaration declaration, Class<T> modelProperty) {
    final Optional<T> property = declaration.getModelProperty(modelProperty);
    assertThat(property.isPresent(), is(true));
    return property.get();
  }

  static void assertLayoutModel(String parameterName, int expectedOrder, Optional<LayoutModel> layoutModel) {
    assertThat(layoutModel.isPresent(), is(true));
    LayoutModel layoutModel1 = layoutModel.get();
    Integer order = layoutModel1.getOrder().orElse(null);
    assertThat("expecting parameter [" + parameterName + "] at position [" + expectedOrder + "] but was at [" + order + "]",
               order, is(expectedOrder));
  }

}
