/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager.jdk;

import org.mule.runtime.api.meta.model.ExtensionModel;

/**
 * Validates that a supplied {@link ExtensionModel} is compatible with the Java version Mule is running on
 *
 * @since 4.5.0
 */
public interface ExtensionJdkValidator {

  /**
   * Validates the given {@code extensionModel}.
   * <p>
   * If the validation fails, implementors may either handle it silently or throw a
   * {@link JavaVersionNotSupportedByExtensionException}.
   *
   * @param extensionModel an {@link ExtensionModel}
   * @throws JavaVersionNotSupportedByExtensionException if the validation fails
   */
  void validateJdkSupport(ExtensionModel extensionModel) throws JavaVersionNotSupportedByExtensionException;

}
