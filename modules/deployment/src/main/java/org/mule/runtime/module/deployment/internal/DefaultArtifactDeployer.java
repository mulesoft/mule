/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_ADD_TOOLING_OBJECTS_TO_REGISTRY;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_ENABLE_XML_VALIDATIONS_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.internal.context.ArtifactStoppedPersistenceListener.ARTIFACT_STOPPED_LISTENER;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactoryUtils.withArtifactMuleContext;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.getPersistedArtifactStatusDeploymentProperties;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.getPersistedFlowDeploymentProperties;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.resolveArtifactStatusDeploymentProperties;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.resolveFlowDeploymentProperties;
import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.START_ARTIFACT_ON_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.module.deployment.internal.FlowStoppedDeploymentPersistenceListener.START_FLOW_ON_DEPLOYMENT_PROPERTY;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Boolean.valueOf;
import static java.lang.String.format;
import static java.util.Optional.empty;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.config.MuleDeploymentProperties;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.internal.construct.DefaultFlowBuilder;
import org.mule.runtime.core.internal.context.ArtifactStoppedPersistenceListener;
import org.mule.runtime.core.internal.context.FlowStoppedPersistenceListener;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.deployment.model.api.DeployableArtifact;
import org.mule.runtime.deployment.model.api.DeploymentException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.slf4j.Logger;

public class DefaultArtifactDeployer<T extends DeployableArtifact> implements ArtifactDeployer<T> {

  private static final Logger logger = getLogger(DefaultArtifactDeployer.class);
  private final Map<String, List<FlowStoppedPersistenceListener>> appsFlowStoppedListeners = new ConcurrentHashMap<>();

  private final Supplier<Scheduler> artifactStartExecutor;

  public DefaultArtifactDeployer(Supplier<Scheduler> artifactStartExecutor) {
    this.artifactStartExecutor = artifactStartExecutor;
  }

  @Override
  public void deploy(T artifact, boolean startArtifact) {
    try {
      artifact.install();
      doInit(artifact);
      addFlowStoppedListeners(artifact);
      if (startArtifact && shouldStartArtifactAccordingToPersistedStatus(artifact)) {
        // The purpose of dispatching this to a separate thread is to have a clean call stack when starting the app.
        // This is needed in order to prevent an StackOverflowError when starting apps with really long flows.
        final Future<?> startTask = artifactStartExecutor.get().submit(() -> {
          try {
            artifact.start();
          } catch (Throwable t) {
            artifact.dispose();
            logger.error("Failed to deploy artifact [{}]", artifact.getArtifactName());
            throw t;
          }
        });

        // Wait till start finishes
        try {
          startTask.get();
        } catch (ExecutionException e) {
          throw e.getCause();
        }
      }

      ArtifactStoppedPersistenceListener artifactStoppedDeploymentListener =
          new ArtifactStoppedDeploymentPersistenceListener(artifact.getArtifactName());
      withArtifactMuleContext(artifact, muleContext -> {
        MuleRegistry muleRegistry = ((MuleContextWithRegistry) muleContext).getRegistry();
        muleRegistry.registerObject(ARTIFACT_STOPPED_LISTENER, artifactStoppedDeploymentListener);
      });
    } catch (Throwable t) {
      artifact.dispose();

      if (t instanceof DeploymentException) {
        throw ((DeploymentException) t);
      }

      throw new DeploymentException(createStaticMessage("Failed to deploy artifact [%s]", artifact.getArtifactName()), t);
    }
  }

  private void addFlowStoppedListeners(T artifact) {
    appsFlowStoppedListeners.put(artifact.getArtifactName(), new ArrayList<>());
    if (artifact.getArtifactContext() != null && artifact.getArtifactContext().getRegistry() != null) {
      for (Flow flow : artifact.getArtifactContext().getRegistry().lookupAllByType(Flow.class)) {
        String flowName = flow.getName();
        String artifactName = artifact.getArtifactName();
        ((DefaultFlowBuilder.DefaultFlow) flow)
            .setIsStatePersisted(() -> isStatePersisted(flowName, artifactName));
        FlowStoppedPersistenceListener flowStoppedPersistenceListener =
            new FlowStoppedDeploymentPersistenceListener(flow.getName(), artifact.getArtifactName());
        ((DefaultFlowBuilder.DefaultFlow) flow).addFlowStoppedListener(flowStoppedPersistenceListener);
        appsFlowStoppedListeners.get(artifact.getArtifactName()).add(flowStoppedPersistenceListener);
      }
    }
  }

  public Boolean isStatePersisted(String flowName, String appName) {
    Optional<Properties> deploymentProperties = getPersistedFlowDeploymentProperties(appName);
    return deploymentProperties.isPresent()
        && deploymentProperties.get().getProperty(flowName + "_" + START_FLOW_ON_DEPLOYMENT_PROPERTY) != null;
  }

  /**
   * Initializes the artifact by taking into account deployment properties
   * {@link MuleDeploymentProperties#MULE_LAZY_INIT_DEPLOYMENT_PROPERTY},
   * {@link MuleDeploymentProperties#MULE_LAZY_INIT_ENABLE_XML_VALIDATIONS_DEPLOYMENT_PROPERTY} and
   * {@link MuleDeploymentProperties#MULE_ADD_TOOLING_OBJECTS_TO_REGISTRY}.
   *
   * @param artifact the T artifact to be initialized
   */
  private void doInit(T artifact) {
    boolean lazyInit = false;
    boolean addToolingObjectsToRegistry = false;
    boolean enableXmlValidations = false;
    if (artifact.getDescriptor().getDeploymentProperties().isPresent()) {
      Properties deploymentProperties = artifact.getDescriptor().getDeploymentProperties().get();
      lazyInit = valueOf((String) deploymentProperties.getOrDefault(MULE_LAZY_INIT_DEPLOYMENT_PROPERTY, "false"));
      addToolingObjectsToRegistry =
          valueOf((String) deploymentProperties.getOrDefault(MULE_ADD_TOOLING_OBJECTS_TO_REGISTRY, "false"));
      enableXmlValidations =
          valueOf((String) deploymentProperties.getOrDefault(MULE_LAZY_INIT_ENABLE_XML_VALIDATIONS_DEPLOYMENT_PROPERTY,
                                                             "false"));
    }

    if (lazyInit) {
      if (addToolingObjectsToRegistry) {
        artifact.lazyInitTooling(!enableXmlValidations);
      } else {
        artifact.lazyInit(!enableXmlValidations);
      }
    } else {
      if (addToolingObjectsToRegistry) {
        artifact.initTooling();
      } else {
        artifact.init();
      }
    }
  }

  @Override
  public void undeploy(T artifact) {
    try {
      doNotPersistArtifactStop(artifact);
      tryToStopArtifact(artifact);
      deletePersistence(artifact);
      tryToDisposeArtifact(artifact);
    } catch (Throwable t) {
      if (t instanceof DeploymentException) {
        throw ((DeploymentException) t);
      }

      final String msg = format("Failed to undeployArtifact artifact [%s]", artifact.getArtifactName());
      throw new DeploymentException(createStaticMessage(msg), t);
    }
  }

  private void tryToDisposeArtifact(T artifact) {
    try {
      artifact.dispose();
    } catch (Throwable t) {
      logger.error(format("Unable to cleanly dispose artifact '%s'. Restart Mule if you get errors redeploying this artifact",
                          artifact.getArtifactName()),
                   t);
    }
  }

  private void tryToStopArtifact(T artifact) {

    try {
      artifact.stop();
    } catch (Throwable t) {
      logger.error(format("Unable to cleanly stop artifact '%s'. Restart Mule if you get errors redeploying this artifact",
                          artifact.getArtifactName()),
                   t);
    }
  }

  /**
   * Checks the persisted property START_ARTIFACT_ON_DEPLOYMENT_PROPERTY to know if the artifact should be started or not. If the
   * artifact was purposely stopped and then the mule was restarted, the artifact should maintain its status and not start on
   * deployment.
   */
  private Boolean shouldStartArtifactAccordingToPersistedStatus(T artifact) {
    Optional<Properties> artifactStatusProperties = getPersistedArtifactStatusDeploymentProperties(artifact.getArtifactName());
    return artifactStatusProperties.isPresent()
        && parseBoolean(artifactStatusProperties.get().getProperty(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY, "true"));
  }

  @Override
  public void doNotPersistArtifactStop(T artifact) {
    if (artifact.getArtifactContext() != null && artifact.getArtifactContext().getRegistry() != null) {
      Optional<ArtifactStoppedPersistenceListener> optionalArtifactStoppedListener =
          artifact.getArtifactContext().getRegistry().lookupByName(ARTIFACT_STOPPED_LISTENER);
      optionalArtifactStoppedListener.ifPresent(ArtifactStoppedPersistenceListener::doNotPersist);
    }
  }

  private void deletePersistence(T artifact) {
    if (artifact.getArtifactContext() != null && artifact.getArtifactContext().getRegistry() != null) {
      Optional<ArtifactStoppedPersistenceListener> optionalArtifactStoppedListener =
          artifact.getArtifactContext().getRegistry().lookupByName(ARTIFACT_STOPPED_LISTENER);
      optionalArtifactStoppedListener.ifPresent(ArtifactStoppedPersistenceListener::deletePersistenceProperties);
    }
  }

}
