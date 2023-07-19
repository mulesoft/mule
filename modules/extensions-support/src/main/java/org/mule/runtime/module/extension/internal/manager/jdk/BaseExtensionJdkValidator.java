/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.manager.jdk;

import static org.mule.runtime.api.util.JavaConstants.JAVA_VERSION_8;
import static org.mule.runtime.core.internal.util.JdkVersionUtils.isJava8;

import static java.lang.String.format;
import static java.lang.String.valueOf;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.internal.util.JdkVersionUtils.JdkVersion;

/**
 * Base implementation for {@link ExtensionJdkValidator}
 *
 * @since 4.5.0
 */
abstract class BaseExtensionJdkValidator implements ExtensionJdkValidator {

  protected final JdkVersion runningJdkVersion;
  private final String versionAsString;

  public BaseExtensionJdkValidator(JdkVersion runningJdkVersion) {
    this.runningJdkVersion = runningJdkVersion;
    versionAsString = isJava8(runningJdkVersion) ? JAVA_VERSION_8 : valueOf(runningJdkVersion.getMajor());
  }

  @Override
  public void validateJdkSupport(ExtensionModel extensionModel) {
    if (!isSupported(extensionModel)) {
      onUnsupportedJdkVersion(extensionModel);
    }
  }

  /**
   * @param extensionModel an {@code ExtensionModel} that failed validation
   * @return a user-friendly message about the given {@code extensionModel} failing validation
   */
  protected String getErrorMessageFor(ExtensionModel extensionModel) {
    return format("Extension '%s' does not support Java %s. Supported versions are: %s",
                  extensionModel.getName(),
                  versionAsString,
                  extensionModel.getSupportedJavaVersions());
  }

  /**
   * Handles {@link ExtensionModel} instances that failed validation
   *
   * @param extensionModel a {@link ExtensionModel} that failed validation
   */
  protected abstract void onUnsupportedJdkVersion(ExtensionModel extensionModel);

  private boolean isSupported(ExtensionModel extensionModel) {
    return extensionModel.getSupportedJavaVersions().contains(versionAsString);
  }

}
