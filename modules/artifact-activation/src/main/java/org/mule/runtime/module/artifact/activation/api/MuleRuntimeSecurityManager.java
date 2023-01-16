/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api;

import static java.lang.Thread.currentThread;

import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;

import java.security.Permission;

public final class MuleRuntimeSecurityManager extends SecurityManager {

  @Override
  public void checkPermission(Permission perm) {
    // allow everything done as part of a permission check down on the stack
    if (this.getClassContext()[3].equals(MuleRuntimeSecurityManager.class)) {
      return;
    }

    if (isDeployableArtifactClassLoader(currentThread().getContextClassLoader())) {
      super.checkPermission(perm);
      // return;
    }

    // for (Class ctxCls : this.getClassContext()) {
    // // if (ctxCls.equals(MuleSecurityManager.class)) {
    // // return;
    // // }
    // ClassLoader clCtxClassLoader = ctxCls.getClassLoader();
    // if (clCtxClassLoader != null) {
    // if (isDeployableArtifactClassLoader(clCtxClassLoader)) {
    // super.checkPermission(perm);
    // return;
    // }
    // }
    // }
  }

  private boolean isDeployableArtifactClassLoader(ClassLoader classLoader) {
    return classLoader != null
        && (classLoader instanceof RegionClassLoader || classLoader.getParent() instanceof RegionClassLoader);
  }
}
