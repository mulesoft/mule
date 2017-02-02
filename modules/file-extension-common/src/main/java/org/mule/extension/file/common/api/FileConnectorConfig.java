/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.common.api;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;
import org.mule.runtime.extension.api.annotation.param.ConfigName;
import org.mule.runtime.extension.api.annotation.param.DefaultEncoding;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

/**
 * Generic contract for a config of a connector which operates over a {@link FileSystem}
 *
 * @since 4.0
 */
public abstract class FileConnectorConfig {

  @ConfigName
  private String configName;

  /**
   * The encoding to use by default when writing contents of type {@link String}. If not specified, it defaults to the default
   * encoding in the mule configuration
   */
  @Parameter
  @DefaultEncoding
  @Placement(tab = ADVANCED_TAB)
  private String defaultWriteEncoding;

  /**
   * @return the name that this config has on the mule registry
   */
  protected String getConfigName() {
    return configName;
  }

  public String getDefaultWriteEncoding() {
    return defaultWriteEncoding;
  }
}
