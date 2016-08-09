/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer.model.runtime;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ConfigurationElement;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ExtensionElement;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ParameterizableTypeElement;

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
   * @return A list {@link ConfigurationElement} of declared configurations
   */
  public List<ConfigurationElement> getConfigurations() {
    final Optional<Configurations> optionalConfigurations = this.getAnnotation(Configurations.class);
    if (optionalConfigurations.isPresent()) {
      final Configurations configurations = optionalConfigurations.get();
      return Stream.of(configurations.value()).map(ConfigurationWrapper::new).collect(toList());
    }
    return emptyList();
  }
}
