/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.api;

import static org.mule.runtime.core.util.StringUtils.EMPTY;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

/**
 * A simple object to configure internationalization.
 *
 * @since 3.7.0
 */
@Alias("i18n")
public final class I18NConfig {

  /**
   * The path to a bundle file containing the messages. If {@code null} then the platform will choose a default one
   */
  @Parameter
  private String bundlePath;

  /**
   * The locale of the {@link #bundlePath}. If {@code null} the platform will choose the system default
   */
  @Parameter
  @Optional(defaultValue = EMPTY)
  private String locale;

  public String getBundlePath() {
    return bundlePath;
  }

  public String getLocale() {
    return locale;
  }
}
