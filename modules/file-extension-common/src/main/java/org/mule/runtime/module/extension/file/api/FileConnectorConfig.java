/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.file.api;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.ConfigName;
import org.mule.runtime.extension.api.annotation.param.Optional;

import javax.inject.Inject;

/**
 * Generic contract for a config of a connector which operates over a {@link FileSystem}
 *
 * @since 4.0
 */
public abstract class FileConnectorConfig implements Initialisable {

  @Inject
  protected MuleContext muleContext;

  @ConfigName
  private String configName;

  /**
   * The encoding to use by default when writing contents of type {@link String}. If not specified, it defaults to the default
   * encoding in the mule configuration
   */
  @Parameter
  @Optional
  private String defaultWriteEncoding;

  /**
   * The directory to be considered as the root of every relative path used with this connector.
   */
  public abstract String getWorkingDir();

  /**
   * @return the name that this config has on the mule registry
   */
  protected String getConfigName() {
    return configName;
  }

  @Override
  public void initialise() throws InitialisationException {
    if (defaultWriteEncoding == null) {
      defaultWriteEncoding = muleContext.getConfiguration().getDefaultEncoding();
    }

    doInitialise();
  }

  protected void doInitialise() throws InitialisationException {}

  public String getDefaultWriteEncoding() {
    return defaultWriteEncoding;
  }
}
