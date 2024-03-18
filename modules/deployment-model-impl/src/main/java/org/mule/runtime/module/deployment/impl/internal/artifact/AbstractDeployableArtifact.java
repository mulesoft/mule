/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.artifact;

import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.internal.context.ArtifactStoppedPersistenceListener.ARTIFACT_STOPPED_LISTENER;
import static org.mule.runtime.core.internal.util.splash.SplashScreen.miniSplash;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.internal.construct.DefaultFlowBuilder;
import org.mule.runtime.core.internal.context.ArtifactStoppedPersistenceListener;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.deployment.model.api.DeployableArtifact;
import org.mule.runtime.deployment.model.api.DeploymentStopException;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.DisposableClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;

import org.slf4j.Logger;

/**
 * Common behaviour for deployable artifacts to handle the stop/dispose phases.
 */
public abstract class AbstractDeployableArtifact<D extends DeployableArtifactDescriptor> implements DeployableArtifact<D> {

  private static final Logger LOGGER = getLogger(AbstractDeployableArtifact.class);
  private static final Logger SPLASH_LOGGER = getLogger("org.mule.runtime.core.internal.logging");

  protected final String shortArtifactType;
  protected final String artifactType;

  protected ArtifactContext artifactContext;
  protected ArtifactClassLoader deploymentClassLoader;

  protected static final String START = "start";
  protected static final String STOP = "stop";

  public AbstractDeployableArtifact(String shortArtifactType, String artifactType, ArtifactClassLoader deploymentClassLoader) {
    this.artifactType = artifactType;
    this.shortArtifactType = shortArtifactType;
    this.deploymentClassLoader = deploymentClassLoader;
  }

  @Override
  public final void stop() {
    if (getArtifactContext() != null && getArtifactContext().getRegistry() != null) {
      for (Flow flow : getArtifactContext().getRegistry().lookupAllByType(Flow.class)) {
        ((DefaultFlowBuilder.DefaultFlow) flow).doNotPersist();
      }
    }

    if (this.artifactContext == null
        || !this.artifactContext.getMuleContext().getLifecycleManager().isDirectTransition(Stoppable.PHASE_NAME)) {
      // domain never started, maybe due to a previous error
      if (SPLASH_LOGGER.isInfoEnabled()) {
        SPLASH_LOGGER.info(format("Stopping %s '%s' with no mule context", shortArtifactType, getArtifactName()));
      }
      return;
    }

    artifactContext.getMuleContext().getLifecycleManager().checkPhase(Stoppable.PHASE_NAME);

    withContextClassLoader(null, () -> {
      if (SPLASH_LOGGER.isInfoEnabled()) {
        SPLASH_LOGGER.info(miniSplash(format("Stopping %s '%s'", artifactType, getArtifactName())));
      }
    });

    withContextClassLoader(deploymentClassLoader.getClassLoader(), () -> {
      this.artifactContext.getMuleContext().stop();
      persistArtifactState(STOP);
      return null;
    });
  }

  @Override
  public final void dispose() {
    withContextClassLoader(null,
                           () -> SPLASH_LOGGER.info(miniSplash(format("Disposing %s '%s'", artifactType, getArtifactName()))));

    // moved wrapper logic into the actual implementation, as redeploy() invokes it directly, bypassing
    // classloader cleanup
    ClassLoader originalClassLoader = currentThread().getContextClassLoader();
    try {
      ClassLoader artifactCL = null;
      if (getArtifactClassLoader() != null) {
        artifactCL = getArtifactClassLoader().getClassLoader();
      }
      // if not initialized yet, it can be null
      if (artifactCL != null) {
        currentThread().setContextClassLoader(artifactCL);
      }

      doDispose();

      if (artifactCL != null) {
        if (artifactCL instanceof DisposableClassLoader) {
          ((DisposableClassLoader) artifactCL).dispose();
        }
      }
    } finally {
      // kill any refs to the old classloader to avoid leaks
      currentThread().setContextClassLoader(originalClassLoader);
      deploymentClassLoader = null;
    }
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

  protected void persistArtifactState(String state) {
    ArtifactStoppedPersistenceListener artifactStoppedPersistenceListener =
        ((MuleContextWithRegistry) this.artifactContext.getMuleContext()).getRegistry().lookupObject(ARTIFACT_STOPPED_LISTENER);
    if (artifactStoppedPersistenceListener != null && state.equals(START)) {
      artifactStoppedPersistenceListener.onStart();
    } else if (artifactStoppedPersistenceListener != null && state.equals(STOP)) {
      artifactStoppedPersistenceListener.onStop();
    }
  }

  @Override
  public ArtifactContext getArtifactContext() {
    return artifactContext;
  }
}
