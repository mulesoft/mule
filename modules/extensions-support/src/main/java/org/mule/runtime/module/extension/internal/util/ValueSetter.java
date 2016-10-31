/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;

/**
 * A contract for setting values into an object
 *
 * @since 3.7.0
 */
public interface ValueSetter {

  /**
   * Sets on {@code target} values contained in {@code result}. This method does not guarantee how many of the values contained in
   * {@code result} are actually assigned (if any). Implementations are free to decide using only a sub set of those values or
   * none at all
   *
   * @param target the object on which values are to be set
   * @param result a {@link ResolverSetResult}
   * @throws MuleException
   */
  void set(Object target, ResolverSetResult result) throws MuleException;
}
