/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import static org.mule.runtime.api.util.collection.Collectors.toImmutableList;
import static org.mule.runtime.module.extension.internal.loader.utils.ParameterUtils.getParameterFields;
import static org.mule.runtime.module.extension.internal.loader.utils.ParameterUtils.getParameterGroupFields;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.module.extension.api.loader.java.type.ConfigurationElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.api.loader.java.type.ParameterizableTypeElement;

import java.util.List;
import java.util.stream.Stream;

/**
 * {@link TypeWrapper} specification for classes that are considered as Configurations
 *
 * @since 4.0
 */
class ConfigurationWrapper extends ComponentWrapper implements ConfigurationElement, ParameterizableTypeElement {

  private final List<ExtensionParameter> parameters;

  ConfigurationWrapper(Class aClass, ClassTypeLoader typeLoader) {
    super(aClass, typeLoader);
    List<FieldElement> parameterFields = getParameterFields(this);
    List<FieldElement> parameterGroupFields = getParameterGroupFields(this);
    this.parameters = Stream.concat(parameterFields.stream(), parameterGroupFields.stream())
        .distinct()
        .collect(toImmutableList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<ExtensionParameter> getParameters() {
    return parameters;
  }
}
