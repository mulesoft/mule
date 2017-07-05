/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import static java.util.stream.Collectors.toList;

import org.mule.runtime.module.extension.internal.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.internal.loader.java.type.OperationContainerElement;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * {@link TypeWrapper} specification for classes that contains Operations inside of it
 *
 * @since 4.0
 */
final class OperationContainerWrapper extends TypeWrapper implements OperationContainerElement {

  private final Class aClass;

  OperationContainerWrapper(Class aClass) {
    super(aClass);
    this.aClass = aClass;
  }

  /**
   * @return The list of {@link MethodWrapper} that the this type holds
   */
  @Override
  public List<MethodElement> getOperations() {
    return Stream.of(aClass).map(IntrospectionUtils::getApiMethods).flatMap(Collection::stream).map(MethodWrapper::new)
        .collect(toList());
  }
}
