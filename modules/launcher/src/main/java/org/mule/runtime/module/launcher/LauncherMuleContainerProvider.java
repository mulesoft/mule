/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.launcher;

import org.mule.runtime.module.reboot.internal.MuleContainer;
import org.mule.runtime.module.reboot.internal.MuleContainerProvider;

public class LauncherMuleContainerProvider implements MuleContainerProvider {

  @Override
  public MuleContainer provide(String[] args) throws Exception {
    return new DefaultMuleContainer(args);
  }
}
