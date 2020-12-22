/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.extension.api.annotation.ExpressionFunctions;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.module.extension.api.loader.java.type.ComponentElement;
import org.mule.runtime.module.extension.api.loader.java.type.ConnectionProviderElement;
import org.mule.runtime.module.extension.api.loader.java.type.FunctionContainerElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationContainerElement;
import org.mule.runtime.module.extension.api.loader.java.type.SourceElement;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Abstract implementation of {@link ComponentWrapper}
 *
 * @since 4.0
 */
abstract class ComponentWrapper extends TypeWrapper implements ComponentElement {

  ComponentWrapper(Class<?> aClass, ClassTypeLoader typeLoader) {
    super(aClass, typeLoader);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<SourceElement> getSources() {
    List<SourceElement> elements = new ArrayList<>();
    collectSources(elements, Sources.class, Sources::value);
    collectSources(elements, org.mule.sdk.api.annotation.Sources.class, org.mule.sdk.api.annotation.Sources::value);

    return elements;
  }

  private <A extends Annotation> void collectSources(List<SourceElement> accumulator,
                                                     Class<A> annotationClass,
                                                     Function<A, Class[]> extractionFunction) {
    getAnnotation(annotationClass)
        .map(a -> stream(extractionFunction.apply(a)))
        .ifPresent(stream -> stream.forEach(s -> accumulator.add(new SourceTypeWrapper(s, typeLoader))));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<OperationContainerElement> getOperationContainers() {

    final Optional<Operations> optionalOperations = this.getAnnotation(Operations.class);
    if (optionalOperations.isPresent()) {
      return stream(optionalOperations.get().value()).map((Class<?> aClass) -> new OperationContainerWrapper(aClass, typeLoader))
          .collect(toList());
    }
    return emptyList();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<FunctionContainerElement> getFunctionContainers() {

    final Optional<ExpressionFunctions> functions = this.getAnnotation(ExpressionFunctions.class);
    if (functions.isPresent()) {
      return stream(functions.get().value()).map((Class<?> aClass) -> new FunctionContainerWrapper(aClass, typeLoader))
          .collect(toList());
    }
    return emptyList();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<ConnectionProviderElement> getConnectionProviders() {

    final Optional<ConnectionProviders> optionalProviders = this.getAnnotation(ConnectionProviders.class);
    if (optionalProviders.isPresent()) {
      return stream(optionalProviders.get().value()).map(c -> new ConnectionProviderTypeWrapper(c, typeLoader)).collect(toList());
    }
    return emptyList();
  }
}
