/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.artifact.classloader;

import static java.lang.Class.forName;

import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ResourceReleaser;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

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
      LOGGER.warn("Unexpected error while cleaning MVEL's ASMAccessorOptimizer and DynamicOptimizer class loaders", t);
    }
  }

  protected void releaseFromASMAccessOptimizer() {
    ClassLoader mvelCl;
    try {
      mvelCl = (ClassLoader) forName("org.mule.mvel2.optimizers.impl.asm.ASMAccessorOptimizer")
          .getMethod("getMVELClassLoader").invoke(null);
    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException error) {
      // MVEL is not present in the class loader, nothing to do
      return;
    }

    try {
      if (mvelCl != null && mvelCl.getParent() == muleArtifactClassLoader) {
        forName("org.mule.mvel2.optimizers.impl.asm.ASMAccessorOptimizer")
            .getMethod("setMVELClassLoader", forName("org.mule.mvel2.util.MVELClassLoader")).invoke(null, new Object[] {null});
      }
    } catch (Throwable t) {
      LOGGER.warn("Unable to clean MVEL's ASMAccessorOptimizer class loader", t);
    }
  }

  protected void releaseFromDynamicOptimizer() {
    Field clField;
    try {
      clField = forName("org.mule.mvel2.optimizers.dynamic.DynamicOptimizer").getDeclaredField("classLoader");
    } catch (ClassNotFoundException | NoSuchFieldException | SecurityException error) {
      // MVEL is not present in the class loader, nothing to do
      return;
    }

    boolean accessible = clField.isAccessible();
    clField.setAccessible(true);
    try {
      ClassLoader mvelCl = (ClassLoader) clField.get(null);

      if (mvelCl != null && mvelCl.getParent() == muleArtifactClassLoader) {
        clField.set(null, null);
      }
    } catch (Throwable t) {
      LOGGER.warn("Unable to clean MVEL's DynamicOptimizer class loader", t);
    } finally {
      clField.setAccessible(accessible);
    }

  }

}
