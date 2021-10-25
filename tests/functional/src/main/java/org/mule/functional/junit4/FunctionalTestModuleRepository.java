/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import static java.lang.System.currentTimeMillis;
import static java.nio.file.Files.createTempDirectory;

import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.container.internal.ClasspathModuleDiscoverer;
import org.mule.runtime.container.internal.CompositeModuleDiscoverer;
import org.mule.runtime.container.internal.ContainerClassLoaderFactory;
import org.mule.runtime.container.internal.DefaultModuleRepository;
import org.mule.runtime.container.internal.JreModuleDiscoverer;
import org.mule.runtime.container.internal.ModuleDiscoverer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation that uses internal classes and should only be used on context of FunctionalTestCases and not in an isolated
 * MuleArtifactFunctionalTestCase.
 */
public class FunctionalTestModuleRepository implements ModuleRepository {

  private final ModuleRepository moduleRepository;

  public FunctionalTestModuleRepository() {
    moduleRepository =
        new DefaultModuleRepository(new TestContainerModuleDiscoverer(ContainerClassLoaderFactory.class.getClassLoader()));
  }

  /**
   * Test module discover that uses a custom implementation of a classpath module discovered in order to use a temporary folder
   * for service module files.
   */
  private class TestContainerModuleDiscoverer implements ModuleDiscoverer {

    private final CompositeModuleDiscoverer moduleDiscoverer;

    public TestContainerModuleDiscoverer(ClassLoader containerClassLoader) {
      checkArgument(containerClassLoader != null, "containerClassLoader cannot be null");
      moduleDiscoverer =
          new CompositeModuleDiscoverer(getModuleDiscoverers(containerClassLoader).toArray(new ModuleDiscoverer[0]));
    }

    protected List<ModuleDiscoverer> getModuleDiscoverers(ClassLoader containerClassLoader) {
      List<ModuleDiscoverer> result = new ArrayList<>();
      result.add(new JreModuleDiscoverer());

      try {
        File temp = createTempDirectory("" + currentTimeMillis()).toFile();
        temp.deleteOnExit();
        result.add(new ClasspathModuleDiscoverer(containerClassLoader, temp));
        return result;
      } catch (IOException e) {
        throw new IllegalStateException("Cannot create temo dir", e);
      }

    }

    @Override
    public List<MuleModule> discover() {
      return moduleDiscoverer.discover();
    }
  }



  @Override
  public List<MuleModule> getModules() {
    return moduleRepository.getModules();
  }
}
