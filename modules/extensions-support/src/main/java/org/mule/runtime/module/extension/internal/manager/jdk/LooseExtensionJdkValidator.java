/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.manager.jdk;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.internal.util.JdkVersionUtils.JdkVersion;

import org.slf4j.Logger;

/**
 * {@link ExtensionJdkValidator} implementation that logs validation misses but doesn't throw any exceptions
 *
 * @since 4.5.0
 */
public class LooseExtensionJdkValidator extends BaseExtensionJdkValidator {

  private final Logger logger;

  public LooseExtensionJdkValidator(JdkVersion runningJdkVersion, Logger logger) {
    super(runningJdkVersion);
    this.logger = logger;
  }

  @Override
  protected void onUnsupportedJdkVersion(ExtensionModel extensionModel) {
    if (logger.isWarnEnabled()) {
      logger.warn(getErrorMessageFor(extensionModel));
    }
  }
}
