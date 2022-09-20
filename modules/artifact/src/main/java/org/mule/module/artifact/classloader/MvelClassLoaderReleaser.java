/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.artifact.classloader;

import static org.mule.mvel2.optimizers.impl.asm.ASMAccessorOptimizer.getMVELClassLoader;
import static org.mule.mvel2.optimizers.impl.asm.ASMAccessorOptimizer.setMVELClassLoader;

import org.mule.mvel2.optimizers.dynamic.DynamicOptimizer;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ResourceReleaser;

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MvelClassLoaderReleaser implements ResourceReleaser {

  private static final Logger LOGGER = LoggerFactory.getLogger(MvelClassLoaderReleaser.class);

  private final MuleArtifactClassLoader muleArtifactClassLoader;

  public MvelClassLoaderReleaser(MuleArtifactClassLoader muleArtifactClassLoader) {
    this.muleArtifactClassLoader = muleArtifactClassLoader;
  }

  @Override
  public void release() {
    try {
      releaseFromASMAccessOptimizer();
      releaseFromDynamicOptimizer();
    } catch (Throwable t) {
      LOGGER.warn("Unable to clean MVEL's DynamicOptimizer ClassLoaders", t);
    }
  }

  protected void releaseFromASMAccessOptimizer() {
    ClassLoader mvelCl = ((ClassLoader) getMVELClassLoader());

    if (mvelCl.getParent() == muleArtifactClassLoader) {
      setMVELClassLoader(null);
    }
  }

  protected void releaseFromDynamicOptimizer() throws Exception {
    ClassLoader mvelCl;
    Field clField = DynamicOptimizer.class.getDeclaredField("classLoader");
    boolean accessible = clField.isAccessible();
    clField.setAccessible(true);
    try {
      mvelCl = (ClassLoader) clField.get(null);

      if (mvelCl.getParent() == muleArtifactClassLoader) {
        clField.set(null, null);
      }
    } finally {
      clField.setAccessible(accessible);
    }

  }

}
