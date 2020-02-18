/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.artifact;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.mule.runtime.core.api.util.ClassUtils.setContextClassLoader;
import static org.mule.runtime.core.internal.logging.LogUtil.log;
import static org.mule.runtime.core.internal.util.splash.SplashScreen.miniSplash;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.deployment.model.api.DeployableArtifact;
import org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor;
import org.mule.runtime.deployment.model.api.DeploymentStopException;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.DisposableClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common behaviour for deployable artifacts to handle the stop/dispose phases.
 */
public abstract class AbstractDeployableArtifact<D extends DeployableArtifactDescriptor> implements DeployableArtifact<D> {

  protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractDeployableArtifact.class);

  protected final String shortArtifactType;
  protected final String artifactType;

  protected ArtifactContext artifactContext;
  protected ArtifactClassLoader deploymentClassLoader;

  public AbstractDeployableArtifact(String shortArtifactType, String artifactType, ArtifactClassLoader deploymentClassLoader) {
    this.artifactType = artifactType;
    this.shortArtifactType = shortArtifactType;
    this.deploymentClassLoader = deploymentClassLoader;
  }

  @Override
  public final void stop() {
    if (this.artifactContext == null
        || !this.artifactContext.getMuleContext().getLifecycleManager().isDirectTransition(Stoppable.PHASE_NAME)) {
      // domain never started, maybe due to a previous error
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info(format("Stopping %s '%s' with no mule context", shortArtifactType, getArtifactName()));
      }
      return;
    }

    artifactContext.getMuleContext().getLifecycleManager().checkPhase(Stoppable.PHASE_NAME);

    Thread currentThread = currentThread();
    ClassLoader originalTCCL = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(null);
    try {
      if (LOGGER.isInfoEnabled()) {
        log(miniSplash(format("Stopping %s '%s'", artifactType, getArtifactName())));
      }
    } finally {
      currentThread.setContextClassLoader(originalTCCL);
    }

    setContextClassLoader(currentThread, originalTCCL, deploymentClassLoader.getClassLoader());
    try {
      artifactContext.getMuleContext().stop();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    } finally {
      setContextClassLoader(currentThread, deploymentClassLoader.getClassLoader(), originalTCCL);
    }
  }

  @Override
  public final void dispose() {
    Thread currentThread = currentThread();
    final ClassLoader originalClassLoader = currentThread().getContextClassLoader();
    currentThread.setContextClassLoader(null);
    try {
      log(miniSplash(format("Disposing %s '%s'", artifactType, getArtifactName())));
    } finally {
      currentThread.setContextClassLoader(originalClassLoader);
    }

    // moved wrapper logic into the actual implementation, as redeploy() invokes it directly, bypassing
    // classloader cleanup

    try {
      ClassLoader artifactCL = null;
      if (getArtifactClassLoader() != null) {
        artifactCL = getArtifactClassLoader().getClassLoader();
      }
      // if not initialized yet, it can be null
      if (artifactCL != null) {
        setContextClassLoader(currentThread, originalClassLoader, artifactCL);
      }

      doDispose();

      if (artifactCL != null) {
        if (isRegionClassLoaderMember(artifactCL)) {
          ((DisposableClassLoader) artifactCL.getParent()).dispose();
        } else if (artifactCL instanceof DisposableClassLoader) {
          ((DisposableClassLoader) artifactCL).dispose();
        }
      }
    } finally {
      // kill any refs to the old classloader to avoid leaks
      currentThread.setContextClassLoader(originalClassLoader);
      deploymentClassLoader = null;
    }
  }

  private static boolean isRegionClassLoaderMember(ClassLoader classLoader) {
    return !(classLoader instanceof RegionClassLoader) && classLoader.getParent() instanceof RegionClassLoader;
  }

  private void doDispose() {
    if (artifactContext == null) {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info(format("%s '%s' never started, nothing to dispose of", capitalize(artifactType), getArtifactName()));
      }
      return;
    }

    try {
      stop();
    } catch (DeploymentStopException e) {
      // catch the stop errors and just log, we're disposing of an domain anyway
      LOGGER.error(format("Error stopping %s '%s'", artifactType, getArtifactName()), e);
    }

    artifactContext.getMuleContext().dispose();
    artifactContext = null;
  }

}
