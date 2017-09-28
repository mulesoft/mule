/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl;

import org.mule.runtime.dsl.api.component.AttributeDefinition;

import java.util.Map;

/**
 * Implementations of this interface are used to define common configuration attribute across a set of {@link org.mule.runtime.dsl.api.component.ComponentBuildingDefinition}s.
 * For instance, all transports support attributes like encoding, returnClass, mimeType and the creation of those attributes
 * depend on the some logic around the values of those parameters.
 * <p>
 * This class can be configured in {@link ConfigurableObjectFactory} as a fixed field to be injected using
 * {@link AttributeDefinition.Builder#fromFixedValue(Object)}.
 *
 * @param <T> type of the object to configure.
 * @since 4.0
 */
public interface ObjectFactoryCommonConfigurator<T> {

  /**
   * Configures the {@code object} instance using the {@code parameters} populated from the configuration.
   *
   * @param object the instance to be configured
   * @param parameters the set of parameters configured in the component model according to the
   *        {@link org.mule.runtime.dsl.api.component.ComponentBuildingDefinition}
   */
  void configure(T object, Map<String, Object> parameters);

}
