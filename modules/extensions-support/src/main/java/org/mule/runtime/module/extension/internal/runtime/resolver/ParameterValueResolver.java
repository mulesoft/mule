/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.module.extension.internal.runtime.ValueResolvingException;

/**
 * Contract for extension components that knows how to resolve parameter values
 *
 * @since 4.0
 */
public interface ParameterValueResolver {

  /**
   * @return The parameter value
   * @throws ValueResolvingException if the resolution fails
   */
  Object getParameterValue(String parameterName) throws ValueResolvingException;
}
