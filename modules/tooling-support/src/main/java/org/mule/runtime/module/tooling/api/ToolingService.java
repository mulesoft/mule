/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.api;

import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingServiceBuilder;

import java.io.File;
import java.io.IOException;

/**
 * Set of services used by tooling to exercise some mule configuration like doing connectivity testing.
 */
public interface ToolingService {

  /**
   * Provides a service to create a connectivity testing service using a builder which can be used to configured resources of a
   * dynamically created artifact
   *
   * @return a builder to create a {@link ConnectivityTestingService}
   */
  ConnectivityTestingServiceBuilder newConnectivityTestingServiceBuilder();

  /**
   * Creates an {@link Application} from a set of resources.
   *
   * The created application will be created lazily meaning that the application resources
   * will be created based on the different request made to the application.
   *
   * Only requested components will be executed. All sources for flows will be stop unless
   * they are requested to be started by the client.
   *
   * The application will be deployed using {@link org.mule.runtime.core.api.config.MuleDeploymentProperties#MULE_MUTE_APP_LOGS_DEPLOYMENT_PROPERTY}
   * so the application logs are muted
   *
   * @param applicationLocation location of the application content. The application content
   *                            may be a folder holding an exploded structure for an application
   *                            or may be a zip file containing the resources of the application.
   * @return the created application.
   */
  //TODO MULE-9703 - improve ToolingService API
  Application createApplication(File applicationLocation) throws IOException;

}
