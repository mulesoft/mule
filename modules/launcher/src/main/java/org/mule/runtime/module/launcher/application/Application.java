/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.application;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.launcher.artifact.DeployableArtifact;
import org.mule.runtime.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.launcher.domain.Domain;

public interface Application extends DeployableArtifact<ApplicationDescriptor> {

  MuleContext getMuleContext();

  /**
   * @return the domain associated with the application.
   */
  Domain getDomain();

  /**
   * @return the current status of the application
   */
  ApplicationStatus getStatus();

}
