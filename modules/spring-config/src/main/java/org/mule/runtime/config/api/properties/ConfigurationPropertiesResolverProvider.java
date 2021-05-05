/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.properties;

import org.mule.runtime.api.component.ConfigurationProperties;

import java.util.function.UnaryOperator;

/**
 * Specialization of {@link ConfigurationProperties} that may provide instances of a {@link UnaryOperator<String>} instance for
 * resolving configuration properties that handle configuration keys consistently.
 * <p>
 * For instance, for an implementation of this whose {@code resolveProperty} method resolves {@code '${someProp}' ->
 * 'SomeValue!'}, the returned {@link UnaryOperator<String>} will resolve {@code 'The value is ${someProp}' -> 'The value is
 * SomeValue!'}
 *
 * @since 4.4
 */
public interface ConfigurationPropertiesResolverProvider extends ConfigurationProperties {

  /**
   * Implementations must ensure that many calls to this method on the same object return the same value.
   *
   * @return a {@link UnaryOperator<String>} instance for resolving configuration properties.
   */
  UnaryOperator<String> getConfigurationPropertiesResolver();

}
