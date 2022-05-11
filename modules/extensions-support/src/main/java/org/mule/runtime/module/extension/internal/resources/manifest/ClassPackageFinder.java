/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.manifest;

import java.util.Optional;

/**
 * Given a {@link Class} name returns the given package where the class belongs.
 *
 * @since 4.1
 */
@FunctionalInterface
public interface ClassPackageFinder {

  /**
   * Given a {@link Class} name returns the given package where the class belongs.
   *
   * @param className The class from which obtain their package.
   * @return An {@link Optional} {@link String} with the package value. Is empty if the package doesn't exist.
   */
  Optional<String> packageFor(String className);
}
