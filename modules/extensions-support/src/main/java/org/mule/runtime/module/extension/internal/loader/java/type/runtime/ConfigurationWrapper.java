/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import org.mule.runtime.core.util.collection.ImmutableSetCollector;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.module.extension.internal.loader.java.type.ConfigurationElement;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.java.type.ParameterizableTypeElement;

import java.util.Set;
import java.util.stream.Stream;

/**
 * {@link TypeWrapper} specification for classes that are considered as Configurations
 *
 * @since 4.0
 */
class ConfigurationWrapper extends ComponentWrapper implements ConfigurationElement, ParameterizableTypeElement {

  private final Set<ExtensionParameter> parameters;

  ConfigurationWrapper(Class aClass) {
    super(aClass);
    this.parameters = Stream.concat(getAnnotatedFields(Parameter.class).stream(),
                                    getAnnotatedFields(ParameterGroup.class).stream())
        .distinct()
        .collect(new ImmutableSetCollector<>());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<ExtensionParameter> getParameters() {
    return parameters;
  }
}
