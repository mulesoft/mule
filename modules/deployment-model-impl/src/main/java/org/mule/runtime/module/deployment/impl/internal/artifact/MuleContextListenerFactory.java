/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
