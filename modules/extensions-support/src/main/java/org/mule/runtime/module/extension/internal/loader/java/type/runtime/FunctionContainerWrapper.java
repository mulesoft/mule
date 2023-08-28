/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import static java.util.stream.Collectors.toList;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.module.extension.api.loader.java.type.FunctionContainerElement;
import org.mule.runtime.module.extension.api.loader.java.type.FunctionElement;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * {@link TypeWrapper} specification for classes that contains Operations inside of it
 *
 * @since 4.0
 */
final class FunctionContainerWrapper extends TypeWrapper implements FunctionContainerElement {

  private final Class aClass;

  FunctionContainerWrapper(Class aClass, ClassTypeLoader typeLoader) {
    super(aClass, typeLoader);
    this.aClass = aClass;
  }

  @Override
  public List<FunctionElement> getFunctions() {
    return Stream.of(aClass).map(IntrospectionUtils::getApiMethods).flatMap(Collection::stream)
        .map((Method method) -> new FunctionWrapper(method, typeLoader))
        .collect(toList());
  }
}
