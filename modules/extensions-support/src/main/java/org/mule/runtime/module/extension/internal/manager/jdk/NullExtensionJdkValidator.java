/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager.jdk;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.internal.util.version.JdkVersionUtils.JdkVersion;

/**
 * Null object implementation of {@link ExtensionJdkValidator}.
 * <p>
 * This implementation doesn't perform any actual validation
 *
 * @since 4.5.0
 */
public class NullExtensionJdkValidator extends BaseExtensionJdkValidator {

  public NullExtensionJdkValidator(JdkVersion runningJdkVersion) {
    super(runningJdkVersion);
  }

  @Override
  public void validateJdkSupport(ExtensionModel extensionModel) {}

  @Override
  protected void onUnsupportedJdkVersion(ExtensionModel extensionModel) {
    // No - Op
  }
}
