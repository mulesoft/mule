/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.FileUtils.deleteTree;
import static org.mule.runtime.core.api.util.FileUtils.unzip;
import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.JAR_FILE_SUFFIX;
import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.ZIP_FILE_SUFFIX;

import static java.nio.file.Files.createTempDirectory;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toSet;

import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.lang3.StringUtils.removeEndIgnoreCase;

import org.mule.runtime.core.api.util.FileUtils;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationStatus;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.internal.util.ObservableList;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deploys domain bundles
 */
public class DomainBundleArchiveDeployer {

  protected static Logger LOGGER = LoggerFactory.getLogger(DomainBundleArchiveDeployer.class);

  private final DeploymentListener deploymentListener;
  private final ArchiveDeployer<DomainDescriptor, Domain> domainDeployer;
  private final ObservableList<Domain> domains;
  private final ArchiveDeployer<ApplicationDescriptor, Application> applicationDeployer;
  private final ObservableList<Application> applications;
  private final DeploymentListener domainDeploymentListener;
  private final CompositeDeploymentListener applicationDeploymentListener;
  private final DeploymentService deploymentService;

  /**
   * Creates a new deployer
   *
   * @param deploymentListener            listener to notify the deployment steps
   * @param domainDeployer                deploys the domains artifacts contained on the domain bundles
   * @param domains                       maintains the deployed domain artifacts
   * @param applicationDeployer           deploys the application artifact contained on the domain bundles
   * @param applications                  maintains the deployed application artifacts
   * @param domainDeploymentListener
   * @param applicationDeploymentListener
   * @param deploymentService
   */
  public DomainBundleArchiveDeployer(DeploymentListener deploymentListener,
                                     ArchiveDeployer<DomainDescriptor, Domain> domainDeployer,
                                     ObservableList<Domain> domains,
                                     ArchiveDeployer<ApplicationDescriptor, Application> applicationDeployer,
                                     ObservableList<Application> applications,
                                     DeploymentListener domainDeploymentListener,
                                     CompositeDeploymentListener applicationDeploymentListener,
                                     DeploymentService deploymentService) {
    this.deploymentListener = deploymentListener;
    this.domainDeployer = domainDeployer;
    this.domains = domains;
    this.applicationDeployer = applicationDeployer;
    this.applications = applications;
    this.domainDeploymentListener = domainDeploymentListener;
    this.applicationDeploymentListener = applicationDeploymentListener;
    this.deploymentService = deploymentService;
  }

  /**
   * Deploys a domain bundle
   *
   * @param uri points to the domain bundle file to be deployed
   * @throws DeploymentException when the domain bundle was not successfully deployed
   */
  public void deployArtifact(URI uri) throws DeploymentException {
    LOGGER.info("deploying artifact: " + uri);
    File bundleFile = new File(uri);
    final String bundleName = removeEndIgnoreCase(bundleFile.getName(), ZIP_FILE_SUFFIX);
    deploymentListener.onDeploymentStart(bundleName);

    File tempFolder = null;
    boolean isRedeploy = false;
    String domainName = null;
    try {
      tempFolder = unzipDomainBundle(bundleFile);
      File domainFile = getDomainFile(tempFolder);
      domainName = getBaseName(domainFile.getName());
      Domain domain = findDomain(domainName);
      isRedeploy = domain != null;

      Set<String> originalAppIds = new HashSet<>();
      if (isRedeploy) {
        domainDeploymentListener.onRedeploymentStart(domainName);

        Collection<Application> originalDomainApplications = deploymentService.findDomainApplications(domainName);
        for (Application domainApplication : originalDomainApplications) {
          if (domainApplication.getStatus() == ApplicationStatus.STARTED
              || domainApplication.getStatus() == ApplicationStatus.STOPPED) {
            applicationDeploymentListener.onRedeploymentStart(domainApplication.getArtifactName());
          }
        }

        originalAppIds = originalDomainApplications.stream().map(a -> a.getArtifactName()).collect(toSet());
      }
      try {
        deployDomain(domainFile);
      } catch (Exception e) {
        // Ignore, deploy applications anyway
        LOGGER.warn("Domain bundle's domain was not deployed", e);
      }

      deployApplications(tempFolder, isRedeploy);

      if (isRedeploy) {
        Collection<Application> newDomainApplications = deploymentService.findDomainApplications(domainName);

        originalAppIds.stream().forEach(appId -> {
          Optional<Application> application =
              newDomainApplications.stream().filter(newApp -> appId.equals(newApp.getArtifactName())).findFirst();
          if (!application.isPresent()) {
            DeploymentException cause = new DeploymentException(
                                                                createStaticMessage("Application was not included in the updated domain bundle"));
            applicationDeploymentListener.onDeploymentFailure(appId, cause);
            applicationDeploymentListener.onRedeploymentFailure(appId, cause);
          }
        });

        domainDeploymentListener.onRedeploymentSuccess(domainName);
      }
      deploymentListener.onDeploymentSuccess(bundleName);
    } catch (Exception e) {
      if (isRedeploy) {
        domainDeploymentListener.onRedeploymentFailure(domainName, e);
      }
      deploymentListener.onDeploymentFailure(bundleName, e);
      if (e instanceof DeploymentException) {
        throw (DeploymentException) e;
      } else {
        throw new DeploymentException(createStaticMessage("Error deploying domain bundle"), e);
      }
    } finally {
      if (tempFolder != null) {
        deleteTree(tempFolder);
      }
    }
  }

  private void deployApplications(File tempFolder, boolean isRedeploy) {
    File applicationsFolder = new File(tempFolder, "applications");
    if (!applicationsFolder.exists()) {
      throw new DeploymentException(createStaticMessage("Domain bundle does not contain an application folder"));
    }

    String[] applicationArtifacts = applicationsFolder.list(new SuffixFileFilter(JAR_FILE_SUFFIX));
    if (applicationArtifacts == null) {
      throw new DeploymentException(createStaticMessage("Domain bundle does not contain applications"));
    }

    Set<String> deployedApps = new HashSet<>();
    boolean applicationDeploymentError = false;
    for (String applicationArtifact : applicationArtifacts) {
      try {
        deployApplication(applicationsFolder, deployedApps, applicationArtifact, isRedeploy);
      } catch (Exception e) {
        applicationDeploymentError = true;
      }
    }

    if (applicationDeploymentError) {
      throw new DeploymentException(createStaticMessage("There was an error deploying the bundled applications"));
    }
  }

  private void deployApplication(File applicationsFolder, Set<String> deployedApps, String applicationArtifact,
                                 boolean isRedeploy) {
    String applicationName = getBaseName(applicationArtifact);
    deployedApps.add(applicationName);

    try {
      applicationDeployer.deployPackagedArtifact(new File(applicationsFolder, applicationArtifact).toURI(), empty());
      applicationDeploymentListener.onRedeploymentSuccess(applicationName);
    } catch (RuntimeException e) {
      if (isRedeploy) {
        applicationDeploymentListener.onRedeploymentFailure(applicationName, e);
      }
      throw e;
    }
  }

  private void deployDomain(File domainFile) throws IOException {
    String domainName = getBaseName(domainFile.getName());
    Domain domain = findDomain(domainName);
    if (domain != null) {
      domainDeployer.undeployArtifact(domainName);
      unzip(domainFile, domain.getLocation());
    }

    domainDeployer.deployPackagedArtifact(domainFile.toURI(), empty());
  }

  private File getDomainFile(File tempFolder) {
    File sourceDomainFolder = new File(tempFolder, "domain");
    String domainFileName = getDomainFileName(sourceDomainFolder);
    return new File(sourceDomainFolder, domainFileName);
  }

  private String getDomainFileName(File sourceDomainFolder) {
    String[] domainFileNames = sourceDomainFolder.list(new SuffixFileFilter(JAR_FILE_SUFFIX));
    if (domainFileNames == null) {
      throw new DeploymentException(createStaticMessage("Domain bundle does not contain a domain artifact"));
    }
    return domainFileNames[0];
  }

  private File unzipDomainBundle(File bundleFile) throws IOException {
    File tempFolder = createTempDirectory(bundleFile.getName()).toFile();
    tempFolder.delete();
    tempFolder.mkdirs();
    FileUtils.unzip(bundleFile, tempFolder);
    bundleFile.delete();
    return tempFolder;
  }

  private Domain findDomain(String domainName) {
    return domains.stream()
        .filter(domain -> domain.getArtifactName().equals(domainName))
        .findAny()
        .orElse(null);
  }
}
