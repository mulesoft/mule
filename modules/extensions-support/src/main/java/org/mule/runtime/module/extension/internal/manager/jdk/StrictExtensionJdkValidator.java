/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.manager.jdk;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.internal.util.JdkVersionUtils.JdkVersion;

/**
 * {@link ExtensionJdkValidator} implementation that throws {@link JavaVersionNotSupportedByExtensionException} upon validation
 * failure
 *
 * @since 4.5.0
 */
public class StrictExtensionJdkValidator extends BaseExtensionJdkValidator {

  public StrictExtensionJdkValidator(JdkVersion runningJdkVersion) {
    super(runningJdkVersion);
  }

  @Override
  protected void onUnsupportedJdkVersion(ExtensionModel extensionModel) {
    throw new JavaVersionNotSupportedByExtensionException(getErrorMessageFor(extensionModel));
  }

}
