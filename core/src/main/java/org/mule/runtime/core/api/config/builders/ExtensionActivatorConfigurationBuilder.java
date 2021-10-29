/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config.builders;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;

public final class ExtensionActivatorConfigurationBuilder extends AbstractConfigurationBuilder {

  @Override
  public void doConfigure(MuleContext muleContext) throws ConfigurationException {
    muleContext.getExtensionManager().activateAllExtensions();
  }
}
