/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.common.api;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.annotation.param.ConfigName;

/**
 * Base class for a {@link ConnectionProvider} which provides instances of
 * {@link FileSystem}
 *
 * @param <T> The generic type of the file system implementation
 * @since 4.0
 */
public abstract class FileSystemProvider<T extends FileSystem> implements ConnectionProvider<T> {

  @ConfigName
  private String configName;

  /**
   * @return the name that this config has on the mule registry
   */
  protected String getConfigName() {
    return configName;
  }

  /**
   * The directory to be considered as the root of every relative path used with this connector.
   */
  public abstract String getWorkingDir();
}
