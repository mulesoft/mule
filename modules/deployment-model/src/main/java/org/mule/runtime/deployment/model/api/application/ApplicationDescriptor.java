/*
/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.application;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.deployment.model.api.domain.DomainDescriptor.MULE_DOMAIN_CLASSIFIER;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class ApplicationDescriptor extends DeployableArtifactDescriptor {

  public static final String DEFAULT_CONFIGURATION_RESOURCE = "mule-config.xml";
  public static final String REPOSITORY_FOLDER = "repository";
  public static final String MULE_APPLICATION_CLASSIFIER = "mule-application";

  private String encoding;
  private Map<String, String> appProperties = new HashMap<String, String>();
  private File logConfigFile;
  private ArtifactDeclaration artifactDeclaration;
  private Optional<BundleDescriptor> domainDescriptor;

  /**
   * Creates a new application descriptor
   *
   * @param name application name. Non empty.
   */
  public ApplicationDescriptor(String name) {
    super(name, empty());
  }

  public ApplicationDescriptor(String name, Optional<Properties> properties) {
    super(name, properties);
  }

  public String getEncoding() {
    return encoding;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  public Map<String, String> getAppProperties() {
    return appProperties;
  }

  public void setAppProperties(Map<String, String> appProperties) {
    this.appProperties = appProperties;
  }

  /**
   * @return the optional descriptor of the domain on wich the application is deployed into
   */
  public Optional<BundleDescriptor> getDomainDescriptor() {
    if (domainDescriptor == null) {
      synchronized (this) {
        if (domainDescriptor == null) {
          Optional<BundleDependency> domain =
              getClassLoaderModel().getDependencies().stream().filter(d -> d.getDescriptor().getClassifier().isPresent()
                  ? d.getDescriptor().getClassifier().get().equals(MULE_DOMAIN_CLASSIFIER) : false).findFirst();
          if (domain.isPresent()) {
            domainDescriptor = ofNullable(domain.get().getDescriptor());
          } else {
            domainDescriptor = Optional.empty();
          }
        }
      }
    }

    return domainDescriptor;
  }

  public void setLogConfigFile(File logConfigFile) {
    this.logConfigFile = logConfigFile;
  }

  public File getLogConfigFile() {
    return logConfigFile;
  }

  /**
   * @return programmatic definition of the application configuration.
   */
  public ArtifactDeclaration getArtifactDeclaration() {
    return artifactDeclaration;
  }

  /**
   * @param artifactDeclaration programmatic definition of the application configuration.
   */
  public void setArtifactDeclaration(ArtifactDeclaration artifactDeclaration) {
    this.artifactDeclaration = artifactDeclaration;
  }

  @Override
  protected List<String> getDefaultConfigResources() {
    return ImmutableList.<String>builder().add(DEFAULT_CONFIGURATION_RESOURCE).build();
  }
}
