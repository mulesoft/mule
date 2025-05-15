/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.api.lifecycle.LifecycleManager;
import org.mule.runtime.core.internal.context.DefaultMuleContextBuilder;

import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Builder that is used to build instances of {@link MuleContext}. Implementing classes are stateful and should provide public
 * chainable setters for configuring the builder instance and no public getters.
 */
@NoImplement
public interface MuleContextBuilder {

  /**
   * Builds a new {@link MuleContext} instance using configured builder instance. Does not initialise or start MuleContext, only
   * constructs the instance.
   */
  MuleContext buildMuleContext();

  void setNotificationManager(ServerNotificationManager notificationManager);

  void setLifecycleManager(LifecycleManager lifecycleManager);

  void setMuleConfiguration(MuleConfiguration muleConfiguration);

  /**
   * @param executionClassLoader classloader to use on the created context. Non null.
   */
  void setExecutionClassLoader(ClassLoader executionClassLoader);

  /**
   * @param objectSerializer object serializer to use on the created context. Non null.
   */
  void setObjectSerializer(ObjectSerializer objectSerializer);

  /**
   * @param errorTypeRepository error type repository to be used in the context, is not configured a default one will be used.
   * @deprecated error type repo is determined from the application, it may not be set. This is a no-op since 4.4.
   */
  @Deprecated
  void setErrorTypeRepository(ErrorTypeRepository errorTypeRepository);


  /**
   * Creates a new {@link MuleContextBuilder} instance
   *
   * @param artifactType type of the artifact the owns the created context.
   * @return a new builder instance
   */
  static MuleContextBuilder builder(ArtifactType artifactType) {
    return new DefaultMuleContextBuilder(artifactType);
  }

  void setDeploymentProperties(Optional<Properties> properties);

  void setListeners(List<MuleContextListener> listeners);

  /**
   * Sets the {@link ArtifactCoordinates} for the deployed app
   *
   * @param artifactCoordinates the app's {@link ArtifactCoordinates}
   * @since 4.5.0
   */
  void setArtifactCoordinates(ArtifactCoordinates artifactCoordinates);

  /**
   * Sets the {@link FeatureFlaggingService} to use instead of the default one.
   *
   * @param featureFlaggingService
   * @since 4.9, 4.8.2
   */
  void setFeatureFlaggingService(Optional<FeatureFlaggingService> featureFlaggingService);

}
