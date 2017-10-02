/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context;

import org.mule.runtime.api.exception.ErrorTypeRepository;
import java.util.Optional;
import java.util.Properties;
import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.api.lifecycle.LifecycleManager;
import org.mule.runtime.core.internal.context.DefaultMuleContextBuilder;

/**
 * Builder that is used to build instances of {@link MuleContext}. Implementing classes are stateful and should provide public
 * chainable setters for configuring the builder instance and no public getters.
 */
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
   */
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
}
