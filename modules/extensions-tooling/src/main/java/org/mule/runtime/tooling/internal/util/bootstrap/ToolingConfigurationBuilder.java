/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tooling.internal.util.bootstrap;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.config.builders.DefaultsConfigurationBuilder;
import org.mule.runtime.core.privileged.registry.RegistrationException;

public class ToolingConfigurationBuilder extends DefaultsConfigurationBuilder {

  @Override
  protected void registerExecutionComponents(MuleContext muleContext) throws RegistrationException {
  }

  //@Override
  //protected void registerMELExpressionLanguage(MuleContext muleContext) throws RegistrationException {
  //}
}
