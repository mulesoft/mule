/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import org.mule.runtime.module.boot.api.MuleContainer;
import org.mule.runtime.module.boot.api.MuleContainerProvider;

/**
 * Default imolementation to create the MuleConteiner.
 *
 * @since 4.6
 */
public class LauncherMuleContainerProvider implements MuleContainerProvider {

  @Override
  public MuleContainer provide() throws Exception {
    return new DefaultMuleContainer();
  }
}
