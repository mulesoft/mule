/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.reboot.internal;

import static org.mule.runtime.jpms.api.JpmsUtils.createModuleLayerClassLoader;
import static org.mule.runtime.jpms.api.MultiLevelClassLoaderFactory.MULTI_LEVEL_URL_CLASSLOADER_FACTORY;

import static java.lang.ClassLoader.getSystemClassLoader;

import org.mule.runtime.module.boot.internal.AbstractMuleContainerFactory;
import org.mule.runtime.module.boot.internal.DefaultMuleClassPathConfig;
import org.mule.runtime.module.boot.internal.MuleContainer;

import java.io.File;
import java.net.URL;

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

  /**
   * Creates the Container's {@link ClassLoader} from the given MULE_HOME and MULE_BASE locations.
   *
   * @param muleHome The location of the MULE_HOME directory.
   * @param muleBase The location of the MULE_BASE directory.
   * @return A {@link ClassLoader} suitable for loading the {@link MuleContainer} class and all its dependencies.
   */
  @Override
  protected ClassLoader createContainerSystemClassLoader(File muleHome, File muleBase) {
    DefaultMuleClassPathConfig config = new DefaultMuleClassPathConfig(muleHome, muleBase);

    return createModuleLayerClassLoader(config.getOptURLs().toArray(new URL[config.getOptURLs().size()]),
                                        config.getMuleURLs().toArray(new URL[config.getMuleURLs().size()]),
                                        MULTI_LEVEL_URL_CLASSLOADER_FACTORY,
                                        getSystemClassLoader());
  }
}
