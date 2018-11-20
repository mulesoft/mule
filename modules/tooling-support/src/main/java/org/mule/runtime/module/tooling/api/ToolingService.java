/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.api;

import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingServiceBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

/**
 * Set of services used by tooling to exercise some mule configuration like doing connectivity testing.
 */
@NoImplement
public interface ToolingService extends Initialisable, Stoppable {

  String DEPLOYMENT_DOMAIN_NAME_REF = "_muleToolingDeploymentDomainNameRef";

  /**
   * Provides a service to create a connectivity testing service using a builder which can be used to configured resources of a
   * dynamically created artifact
   *
   * @return a builder to create a {@link ConnectivityTestingService}
   */
  ConnectivityTestingServiceBuilder newConnectivityTestingServiceBuilder();

  /**
   * Creates an {@link Application} from a set of resources.
   * <p/>
   * The created application will be created lazily meaning that the application resources
   * will be created based on the different request made to the application.
   * <p/>
   * Only requested components will be executed. All sources for flows will be stop unless
   * they are requested to be started by the client.
   * <p/>
   * If a domain dependency is defined for the application it will be deployed. Both domain and application
   * will be deployed using a random unique identifier allowing to get multiple deployments of both application and domain. Be
   * aware that resources (ports, file system, etc) are shared so in those cases where resources are needed by the application or domain a deployment
   * exception could happen.
   * <p/>
   * The application will be deployed using {@link org.mule.runtime.core.api.config.MuleDeploymentProperties#MULE_MUTE_APP_LOGS_DEPLOYMENT_PROPERTY}
   * so the application logs are muted.
   *
   * @param applicationLocation location of the application content. The application content
   *                            may be a folder holding an exploded structure for an application
   *                            or may be a zip file containing the resources of the application.
   * @return the created application.
   * @throws IOException
   */
  Application createApplication(File applicationLocation) throws IOException;

  /**
   * Creates an {@link Application} from a set of resources.
   * <p/>
   * The created application will be created lazily meaning that the application resources
   * will be created based on the different request made to the application.
   * <p/>
   * Only requested components will be executed. All sources for flows will be stop unless
   * they are requested to be started by the client.
   * <p/>
   * If a domain dependency is defined for the application it will be deployed. Both domain and application
   * will be deployed using a random unique identifier allowing to get multiple deployments of both application and domain. Be
   * aware that resources (ports, file system, etc) are shared so in those cases where resources are needed by the application or domain a deployment
   * exception could happen.
   * <p/>
   * The application will be deployed using {@link org.mule.runtime.core.api.config.MuleDeploymentProperties#MULE_MUTE_APP_LOGS_DEPLOYMENT_PROPERTY}
   * so the application logs are muted.
   *
   * @param applicationLocation location of the application content. The application content
   *                            may be a folder holding an exploded structure for an application
   *                            or may be a zip file containing the resources of the application.
   * @param deploymentProperties defines the deployment properties to be used when deploying the application.
   * @return                     the created application.
   * @throws IOException
   */
  default Application createApplication(File applicationLocation, Optional<Properties> deploymentProperties) throws IOException {
    return createApplication(applicationLocation);
  }

  /**
   * Creates an application but from the content byte[], see {@link #createApplication(File)} for more details.
   *
   * @param appContent the content of the application.
   * @return the created application.
   * @throws IOException
   */
  default Application createApplication(byte[] appContent) throws IOException {
    throw new UnsupportedOperationException("Method not support for Mule Runtime version: " + getProductVersion());
  }

  /**
   * Creates an application but from the content byte[], see {@link #createApplication(File, Optional)} for more details.
   *
   * @param appContent the content of the application.
   * @param deploymentProperties defines the deployment properties to be used when deploying the application.
   * @return the created application.
   * @throws IOException
   */
  default Application createApplication(byte[] appContent, Optional<Properties> deploymentProperties) throws IOException {
    throw new UnsupportedOperationException("Method not support for Mule Runtime version: " + getProductVersion());
  }

  /**
   * Creates a {@link org.mule.runtime.deployment.model.api.domain.Domain} from a set of resources.
   * <p/>
   * The created domain will be created lazily meaning that the domain resources
   * will be created based on the different request made to the domain.
   * <p/>
   * Only requested components will be executed. All sources for flows will be stop unless
   * they are requested to be started by the client.
   * <p/>
   * Be aware that resources (ports, file system, etc) are shared so in those cases where resources are needed by the domain a deployment
   * exception could happen.
   * <p/>
   * The domain will be deployed using {@link org.mule.runtime.core.api.config.MuleDeploymentProperties#MULE_MUTE_APP_LOGS_DEPLOYMENT_PROPERTY}
   * so the domain logs are muted.
   *
   * @param domainLocation location of the domain content. The domain content
   *                          may be a folder holding an exploded structure for an domain
   *                          or may be a jar file containing the resources of the domain.
   * @param deploymentProperties defines the deployment properties to be used when deploying the domain.
   * @return the created domain.
   * @throws IOException
   */
  default Domain createDomain(File domainLocation, Optional<Properties> deploymentProperties) throws IOException {
    throw new UnsupportedOperationException("Method not support for Mule Runtime version: " + getProductVersion());
  }

  /**
   * Creates a {@link org.mule.runtime.deployment.model.api.domain.Domain} from a set of resources.
   * <p/>
   * The created domain will be created lazily meaning that the domain resources
   * will be created based on the different request made to the domain.
   * <p/>
   * Only requested components will be executed. All sources for flows will be stop unless
   * they are requested to be started by the client.
   * <p/>
   * Be aware that resources (ports, file system, etc) are shared so in those cases where resources are needed by the domain a deployment
   * exception could happen.
   * <p/>
   * The domain will be deployed using {@link org.mule.runtime.core.api.config.MuleDeploymentProperties#MULE_MUTE_APP_LOGS_DEPLOYMENT_PROPERTY}
   * so the domain logs are muted.
   *
   * @param domainLocation location of the domain content. The domain content
   *                          may be a folder holding an exploded structure for an domain
   *                          or may be a jar file containing the resources of the domain.
   * @return the created domain.
   * @throws IOException
   */
  default Domain createDomain(File domainLocation) throws IOException {
    throw new UnsupportedOperationException("Method not support for Mule Runtime version: " + getProductVersion());
  }

  /**
   * Creates a domain but from the content byte[], see {@link #createDomain(File)} for more details.
   *
   * @param domainContent the content of the domain.
   * @return the created domain.
   * @throws IOException
   */
  default Domain createDomain(byte[] domainContent) throws IOException {
    throw new UnsupportedOperationException("Method not support for Mule Runtime version: " + getProductVersion());
  }

  /**
   * Creates a domain but from the content byte[], see {@link #createDomain(File)} for more details.
   *
   * @param domainContent the content of the domain.
   * @param deploymentProperties defines the deployment properties to be used when deploying the application.
   * @return the created domain.
   * @throws IOException
   */
  default Domain createDomain(byte[] domainContent, Optional<Properties> deploymentProperties) throws IOException {
    throw new UnsupportedOperationException("Method not support for Mule Runtime version: " + getProductVersion());
  }

  default void initialise() throws InitialisationException {}

  default void stop() throws MuleException {}

}
