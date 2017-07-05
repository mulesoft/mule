/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getApiMethods;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.ExpressionFunctions;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.module.extension.internal.loader.java.type.ConfigurationElement;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.internal.loader.java.type.ParameterizableTypeElement;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * {@link ConfigurationWrapper} specification for classes that are considered as Extensions
 *
 * @since 4.0
 */
public class ExtensionTypeWrapper<T> extends ComponentWrapper implements ExtensionElement, ParameterizableTypeElement {

  public ExtensionTypeWrapper(Class<T> aClass) {
    super(aClass);
  }

  /**
   * {@inheritDoc}
   */
  public List<ConfigurationElement> getConfigurations() {
    final Optional<Configurations> optionalConfigurations = this.getAnnotation(Configurations.class);
    if (optionalConfigurations.isPresent()) {
      final Configurations configurations = optionalConfigurations.get();
      return Stream.of(configurations.value()).map(ConfigurationWrapper::new).collect(toList());
    }
    return emptyList();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<MethodElement> getOperations() {
    return getAnnotation(Operations.class)
        .map(classes -> Stream.of(classes.value())
            .flatMap(clazz -> getApiMethods(clazz).stream())
            .map(clazz -> (MethodElement) new MethodWrapper(clazz))
            .collect(toList()))
        .orElse(emptyList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<MethodElement> getFunctions() {
    return getAnnotation(ExpressionFunctions.class)
        .map(classes -> Stream.of(classes.value())
            .flatMap(clazz -> getApiMethods(clazz).stream())
            .map(clazz -> (MethodElement) new MethodWrapper(clazz))
            .collect(toList()))
        .orElse(emptyList());
  }
}
