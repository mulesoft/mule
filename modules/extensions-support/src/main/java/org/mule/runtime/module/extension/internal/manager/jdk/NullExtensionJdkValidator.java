/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.manager.jdk;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.internal.util.JdkVersionUtils.JdkVersion;

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
