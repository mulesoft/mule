/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Boolean.valueOf;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.scheduler.SchedulerConfig.config;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_ENABLE_XML_VALIDATIONS_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.internal.context.ArtifactStoppedPersistenceListener.ARTIFACT_STOPPED_LISTENER;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactoryUtils.withArtifactMuleContext;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.resolveDeploymentProperties;
import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.START_ARTIFACT_ON_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.module.deployment.internal.MuleDeploymentService.useParallelDeployment;
import static org.mule.runtime.module.deployment.internal.ParallelDeploymentDirectoryWatcher.MAX_APPS_IN_PARALLEL_DEPLOYMENT;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

import org.slf4j.Logger;

public class DefaultArtifactDeployer<T extends DeployableArtifact> implements ArtifactDeployer<T> {

  private static final Logger logger = getLogger(DefaultArtifactDeployer.class);
  private final Map<String, List<FlowStoppedPersistenceListener>> appsFlowStoppedListeners = new HashMap<>();

  private final Scheduler threadPoolExecutor;

  public DefaultArtifactDeployer(Supplier<SchedulerService> schedulerServiceSupplier) {
    this.threadPoolExecutor =
        schedulerServiceSupplier.get()
            .customScheduler(config()
                .withName("ArtifactDeployer.start")
                .withMaxConcurrentTasks(useParallelDeployment() ? MAX_APPS_IN_PARALLEL_DEPLOYMENT : 1),
                             256);
  }

  @Override
  public void deploy(T artifact, boolean startArtifact) {
    try {
      artifact.install();
      doInit(artifact);
      addFlowStoppedListeners(artifact);
      if (shouldStartArtifact(artifact)) {
        threadPoolExecutor.execute(() -> {
          try {
            artifact.start();
          } catch (Throwable t) {
            artifact.dispose();
            logger.error("Failed to deploy artifact [{}]", artifact.getArtifactName());
          }
        });
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
    if (artifact.getRegistry() != null) {
      for (Flow flow : artifact.getRegistry().lookupAllByType(Flow.class)) {
        FlowStoppedPersistenceListener flowStoppedPersistenceListener =
            new FlowStoppedDeploymentPersistenceListener(flow.getName(), artifact.getArtifactName());
        ((DefaultFlowBuilder.DefaultFlow) flow).addFlowStoppedListener(flowStoppedPersistenceListener);
        appsFlowStoppedListeners.get(artifact.getArtifactName()).add(flowStoppedPersistenceListener);
      }
    }
  }

  /**
   * Initializes the artifact by taking into account deployment properties
   * {@link org.mule.runtime.core.api.config.MuleDeploymentProperties#MULE_LAZY_INIT_DEPLOYMENT_PROPERTY} and
   * {@link org.mule.runtime.core.api.config.MuleDeploymentProperties#MULE_LAZY_INIT_ENABLE_XML_VALIDATIONS_DEPLOYMENT_PROPERTY}.
   *
   * @param artifact the T artifact to be initialized
   */
  private void doInit(T artifact) {
    boolean lazyInit = false;
    boolean enableXmlValidations = false;
    if (artifact.getDescriptor().getDeploymentProperties().isPresent()) {
      Properties deploymentProperties = artifact.getDescriptor().getDeploymentProperties().get();
      lazyInit = valueOf((String) deploymentProperties.getOrDefault(MULE_LAZY_INIT_DEPLOYMENT_PROPERTY, "false"));
      enableXmlValidations =
          valueOf((String) deploymentProperties.getOrDefault(MULE_LAZY_INIT_ENABLE_XML_VALIDATIONS_DEPLOYMENT_PROPERTY,
                                                             "false"));
    }

    if (lazyInit) {
      artifact.lazyInit(!enableXmlValidations);
    } else {
      artifact.init();
    }
  }

  @Override
  public void undeploy(T artifact) {
    try {
      doNotPersistFlowsStop(artifact.getArtifactName());
      doNotPersistArtifactStop(artifact);
      tryToStopArtifact(artifact);
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

  private Boolean shouldStartArtifact(T artifact) {
    Properties deploymentProperties = null;
    try {
      deploymentProperties = resolveDeploymentProperties(artifact.getArtifactName(), empty());
    } catch (IOException e) {
      logger.error("Failed to load deployment property for artifact "
          + artifact.getArtifactName(), e);
    }
    return deploymentProperties != null
        && parseBoolean(deploymentProperties.getProperty(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY, "true"));
  }

  @Override
  public void doNotPersistArtifactStop(T artifact) {
    Registry artifactRegistry = artifact.getRegistry();

    if (artifactRegistry != null) {
      Optional<ArtifactStoppedPersistenceListener> optionalArtifactStoppedListener =
          artifactRegistry.lookupByName(ARTIFACT_STOPPED_LISTENER);
      optionalArtifactStoppedListener.ifPresent(ArtifactStoppedPersistenceListener::doNotPersist);
    }
  }

  @Override
  public void doNotPersistFlowsStop(String artifactName) {
    if (appsFlowStoppedListeners.containsKey(artifactName)) {
      appsFlowStoppedListeners.get(artifactName)
          .forEach(FlowStoppedPersistenceListener::doNotPersist);
    }
  }

}
