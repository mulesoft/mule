/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.artifact.descriptor.api;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;

import java.util.Map;
import java.util.Optional;

/**
 * Describes a Mule Application artifact.
 *
 * @since 4.9
 */
@NoImplement
public interface ApplicationDescriptor extends DeployableArtifactDescriptor {

  String getEncoding();

  // void setEncoding(String encoding);

  Map<String, String> getAppProperties();

  // void setAppProperties(Map<String, String> appProperties);

  String getDomainName();

  /**
   * @return the optional descriptor of the domain on which the application is deployed into
   */
  Optional<? extends BundleDescriptor> getDomainDescriptor();

  // void setDomainName(String domainName);

  /**
   * @return programmatic definition of the application configuration.
   */
  ArtifactDeclaration getArtifactDeclaration();

  // /**
  // * @param artifactDeclaration programmatic definition of the application configuration.
  // */
  // void setArtifactDeclaration(ArtifactDeclaration artifactDeclaration);

}
