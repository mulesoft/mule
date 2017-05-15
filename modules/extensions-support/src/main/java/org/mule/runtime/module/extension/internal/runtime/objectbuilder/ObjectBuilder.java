/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.objectbuilder;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;

/**
 * A builder capable of creating instances of a given {@link Class}, populating its attributes based on a list of
 * {@link ValueResolver}s.
 * <p/>
 * The built object's class must comply with the following requirements:
 * <p/>
 * <ul>
 * <li>It must be a concrete class
 * <li>It must have a public, default constructor</li>
 * <li>It must have a standard setter for each property that this builder will be populating</li>
 * </ul>
 * <p/>
 * Instances of this class are to be considered thread safe and reusable since the {@link #build(ValueResolvingContext)}
 * method can be invoked several times on the same instance.
 * Each time {@link #build(ValueResolvingContext)} is invoked, a new instance will be created and the resolvers
 * will be re evaluated with the given {@link ValueResolvingContext}.
 *
 * @since 3.7.0
 */
public interface ObjectBuilder<T> {

  /**
   * Whether any of the registered {@link ValueResolver}s are dynamic
   *
   * @return {@code true} if at least one resolver is dynamic. {@code false} otherwise
   */
  boolean isDynamic();

  /**
   * Returns a new instance of the specified class. The given {@link ValueResolvingContext} will be used to obtain a value
   * from each registered {@link ValueResolver}
   *
   * @param context a {@link ValueResolvingContext}
   * @return a new instance
   * @throws {@link MuleException}
   */
  T build(ValueResolvingContext context) throws MuleException;
}
