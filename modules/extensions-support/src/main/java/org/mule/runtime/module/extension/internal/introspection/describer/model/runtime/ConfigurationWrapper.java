/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer.model.runtime;

import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ConfigurationElement;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ExtensionParameter;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ParameterizableTypeElement;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * {@link TypeWrapper} specification for classes that are considered as Configurations
 *
 * @since 4.0
 */
class ConfigurationWrapper extends ComponentWrapper implements ConfigurationElement, ParameterizableTypeElement {

  ConfigurationWrapper(Class aClass) {
    super(aClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<ExtensionParameter> getParameters() {
    return ImmutableList.<ExtensionParameter>builder().addAll(getAnnotatedFields(Parameter.class))
        .addAll(getAnnotatedFields(ParameterGroup.class)).build();
  }
}
