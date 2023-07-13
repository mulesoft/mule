/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager.jdk;

import static org.mule.runtime.api.util.JavaConstants.JAVA_VERSION_8;
import static org.mule.runtime.core.internal.util.JdkVersionUtils.isJava8;

import static java.lang.String.format;
import static java.lang.String.valueOf;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.internal.util.JdkVersionUtils.JdkVersion;

abstract class BaseExtensionJdkValidator implements ExtensionJdkValidator {

  protected final JdkVersion runningJdkVersion;

  public BaseExtensionJdkValidator(JdkVersion runningJdkVersion) {
    this.runningJdkVersion = runningJdkVersion;
  }

  @Override
  public void validateJdkSupport(ExtensionModel extensionModel) {
    if (!isSupported(extensionModel)) {
      onUnsupportedJdkVersion(extensionModel);
    }
  }

  protected String getErrorMessageFor(ExtensionModel extensionModel) {
    return format("Extension '%s' does not support Java %s. Supported versions are: ",
                  extensionModel.getName(),
                  runningJdkVersion.asMajorMinorString(),
                  extensionModel.getSupportedJavaVersions());
  }

  protected abstract void onUnsupportedJdkVersion(ExtensionModel extensionModel);

  private boolean isSupported(ExtensionModel extensionModel) {
    String criteria = isJava8(runningJdkVersion) ? JAVA_VERSION_8 : valueOf(runningJdkVersion.getMajor());
    return extensionModel.getSupportedJavaVersions().contains(criteria);
  }

}
