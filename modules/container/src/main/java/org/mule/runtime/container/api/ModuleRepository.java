/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.api;

import org.mule.runtime.container.internal.ClasspathModuleDiscoverer;
import org.mule.runtime.container.internal.CompositeModuleDiscoverer;
import org.mule.runtime.container.internal.DefaultModuleRepository;
import org.mule.runtime.container.internal.JreModuleDiscoverer;

import java.io.File;
import java.util.List;

/**
 * Provides access to all Mule modules available on the container.
 */
public interface ModuleRepository {

  /**
   * Creates a ModuleRepository based on the modules available on the provided {@code classLoader}.
   * 
   * @param classLoader     where to look for modules.
   * @param temporaryFolder
   * @return
   */
  public static ModuleRepository createModuleRepository(ClassLoader classLoader, File temporaryFolder) {
    return new DefaultModuleRepository(new CompositeModuleDiscoverer(new JreModuleDiscoverer(),
                                                                     new ClasspathModuleDiscoverer(classLoader,
                                                                                                   temporaryFolder)));
  }

  /**
   * @return a non null list of {@link MuleModule}
   */
  List<MuleModule> getModules();
}
