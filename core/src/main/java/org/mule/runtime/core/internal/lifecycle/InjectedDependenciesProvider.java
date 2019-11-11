/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lifecycle;

import org.mule.runtime.api.functional.Either;

import java.util.Collection;

/**
 * Provides information about dependencies that {@code this} component expects to be injected.
 * <p>
 * This is useful in cases that the implementing class is a wrapper of the object that is the actual injection
 * target.
 *
 * @since 4.0.1
 */
public interface InjectedDependenciesProvider {

  /**
   * Returns all the dependencies that are expected to be injected. Each dependency is going to {@link Either}
   * be a {@link Class} or the dependency name.
   */
  Collection<Either<Class<?>, String>> getInjectedDependencies();
}
