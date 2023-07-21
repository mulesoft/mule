/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.impl.internal.artifact;

import org.mule.runtime.core.api.context.notification.MuleContextListener;

/**
 * Creates {@link MuleContextListener} instances
 */
public interface MuleContextListenerFactory {

  /**
   * Creates a context listener for a given artifact
   * 
   * @param artifactName name of the artifact owning the listener. Non empty.
   * @return
   */
  MuleContextListener create(String artifactName);
}
