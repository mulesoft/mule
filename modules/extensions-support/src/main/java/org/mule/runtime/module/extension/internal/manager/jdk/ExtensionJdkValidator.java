/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
