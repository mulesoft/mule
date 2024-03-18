/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static java.lang.String.format;
import static java.util.Optional.empty;

import static org.apache.commons.io.filefilter.DirectoryFileFilter.DIRECTORY;

import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;

import java.io.File;
import java.io.FilenameFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeploymentUtils {

  protected static final Logger LOGGER = LoggerFactory.getLogger(DeploymentUtils.class);

  public DeploymentUtils() {}

  public static String[] listFiles(File directory, FilenameFilter filter) {
    String[] files = directory.list(filter);
    if (files == null) {
      throw new IllegalStateException(format("We got a null while listing the contents of director '%s'. Some common " +
          "causes for this is a lack of permissions to the directory or that it's being deleted concurrently",
                                             directory.getName()));
    }
    return files;
  }

  public static void deployExplodedDomains(ArchiveDeployer<DomainDescriptor, Domain> domainDeployer) {
    String[] domains = listFiles(domainDeployer.getDeploymentDirectory(), DIRECTORY);
    deployExplodedDomains(domainDeployer, domains);
  }

  public static void deployExplodedDomains(ArchiveDeployer<DomainDescriptor, Domain> domainDeployer, String[] domains) {
    for (String addedDomain : domains) {
      try {
        if (domainDeployer.isUpdatedZombieArtifact(addedDomain)) {
          domainDeployer.deployExplodedArtifact(addedDomain, empty());
        }
      } catch (DeploymentException e) {
        LOGGER.error("Error deploying domain '{}'", addedDomain, e);
      }
    }
  }
}
