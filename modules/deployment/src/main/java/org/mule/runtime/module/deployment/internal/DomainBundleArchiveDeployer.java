/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static java.util.Optional.empty;
import static org.apache.commons.collections.CollectionUtils.find;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.lang3.StringUtils.removeEndIgnoreCase;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.FileUtils.deleteTree;
import static org.mule.runtime.core.api.util.FileUtils.unzip;
import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.ARTIFACT_NAME_PROPERTY;
import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.JAR_FILE_SUFFIX;
import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.ZIP_FILE_SUFFIX;
import org.mule.runtime.core.api.util.FileUtils;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.internal.util.ObservableList;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.beanutils.BeanPropertyValueEqualsPredicate;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deploys domain bundles
 */
public class DomainBundleArchiveDeployer {

  protected static Logger LOGGER = LoggerFactory.getLogger(DomainBundleArchiveDeployer.class);

  private final DeploymentListener deploymentListener;
  private final ArchiveDeployer<Domain> domainDeployer;
  private final ObservableList<Domain> domains;
  private final ArchiveDeployer<Application> applicationDeployer;
  private final ObservableList<Application> applications;

  /**
   * Creates a new deployer
   *
   * @param deploymentListener listener to notify the deployment steps
   * @param domainDeployer deploys the domains artifacts contained on the domain bundles
   * @param domains maintains the deployed domain artifacts
   * @param applicationDeployer deploys the application artifact contained on the domain bundles
   * @param applications maintains the deployed application artifacts
   */
  public DomainBundleArchiveDeployer(DeploymentListener deploymentListener, ArchiveDeployer<Domain> domainDeployer,
                                     ObservableList<Domain> domains,
                                     ArchiveDeployer<Application> applicationDeployer,
                                     ObservableList<Application> applications) {
    this.deploymentListener = deploymentListener;
    this.domainDeployer = domainDeployer;
    this.domains = domains;
    this.applicationDeployer = applicationDeployer;
    this.applications = applications;
  }

  /**
   * Deploys a domain bundle
   *
   * @param uri points to the domain bundle file to be deployed
   * @throws DeploymentException when the domain bundle was not successfully deployed
   */
  public void deployArtifact(URI uri) throws DeploymentException {
    File bundleFile = new File(uri);
    final String bundleName = removeEndIgnoreCase(bundleFile.getName(), ZIP_FILE_SUFFIX);
    deploymentListener.onDeploymentStart(bundleName);

    File tempFolder = null;
    try {
      tempFolder = unzipDomainBundle(bundleFile);

      try {
        deployDomain(tempFolder);
      } catch (Exception e) {
        // Ignore, deploy applications anyway
        LOGGER.warn("Domain bundle's domain was not deployed", e);
      }

      deployApplications(tempFolder);

      deploymentListener.onDeploymentSuccess(bundleName);
    } catch (Exception e) {
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

  private void deployApplications(File tempFolder) {
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
        deployApplication(applicationsFolder, deployedApps, applicationArtifact);
      } catch (Exception e) {
        applicationDeploymentError = true;
      }
    }

    if (applicationDeploymentError) {
      throw new DeploymentException(createStaticMessage("There was an error deploying the bundled applications"));
    }
  }

  private void deployApplication(File applicationsFolder, Set<String> deployedApps, String applicationArtifact) {
    String applicationName = getBaseName(applicationArtifact);
    deployedApps.add(applicationName);
    Application application = findApplication(applicationName);
    if (application != null) {
      applicationDeployer.redeploy(application, empty());
    } else {
      applicationDeployer.deployPackagedArtifact(new File(applicationsFolder, applicationArtifact).toURI(), empty());
    }
  }

  private void deployDomain(File tempFolder) throws IOException {
    File sourceDomainFolder = new File(tempFolder, "domain");
    String[] domainFileNames = sourceDomainFolder.list(new SuffixFileFilter(JAR_FILE_SUFFIX));
    if (domainFileNames == null) {
      throw new DeploymentException(createStaticMessage("Domain bundle does not contain a domain artifact"));
    }

    File domainFile = new File(sourceDomainFolder, domainFileNames[0]);

    String domainName = getBaseName(domainFileNames[0]);
    Domain domain = findDomain(domainName);
    if (domain != null) {
      domainDeployer.undeployArtifact(domainName);
      unzip(domainFile, domain.getLocation());
    }

    domainDeployer.deployPackagedArtifact(domainFile.toURI(), empty());
  }

  private File unzipDomainBundle(File bundleFile) throws IOException {
    File tempFolder = File.createTempFile(bundleFile.getName(), "tmp");
    tempFolder.delete();
    tempFolder.mkdirs();
    FileUtils.unzip(bundleFile, tempFolder);
    bundleFile.delete();
    return tempFolder;
  }

  private Domain findDomain(String domainName) {
    return (Domain) find(domains, new BeanPropertyValueEqualsPredicate(ARTIFACT_NAME_PROPERTY, domainName));
  }

  private Application findApplication(String appName) {
    return (Application) find(applications, new BeanPropertyValueEqualsPredicate(ARTIFACT_NAME_PROPERTY, appName));
  }
}
