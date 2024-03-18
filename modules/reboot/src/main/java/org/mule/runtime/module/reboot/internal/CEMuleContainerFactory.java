/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.reboot.internal;

import org.mule.runtime.module.boot.api.MuleContainer;
import org.mule.runtime.module.boot.commons.internal.AbstractMuleContainerFactory;
import org.mule.runtime.module.boot.commons.internal.DefaultMuleClassPathConfig;

import java.io.File;

/**
 * A factory for {@link MuleContainer} instances. Responsible for choosing the right implementation class and setting up its
 * {@link ClassLoader}.
 *
 * @since 4.5
 */
public class CEMuleContainerFactory extends AbstractMuleContainerFactory {

  public CEMuleContainerFactory(String muleHomeDirectoryPropertyName, String muleBaseDirectoryPropertyName) {
    super(muleHomeDirectoryPropertyName, muleBaseDirectoryPropertyName);
  }

  @Override
  protected DefaultMuleClassPathConfig createMuleClassPathConfig(File muleHome, File muleBase) {
    return new DefaultMuleClassPathConfig(muleHome, muleBase);
  }
}
