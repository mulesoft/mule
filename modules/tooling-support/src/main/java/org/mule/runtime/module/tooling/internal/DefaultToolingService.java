/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.container.api.MuleFoldersUtil.getMuleBaseFolder;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_MUTE_APP_LOGS_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.util.FileUtils.cleanDirectory;
import static org.mule.runtime.module.deployment.impl.internal.maven.AbstractMavenClassLoaderModelLoader.CLASSLOADER_MODEL_MAVEN_REACTOR_RESOLVER;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.lookupPomFromMavenLocation;
import org.mule.maven.client.api.MavenReactorResolver;
import org.mule.runtime.api.deployment.meta.MuleApplicationModel;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.util.UUID;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.deployment.impl.internal.application.ApplicationWrapper;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.deployment.impl.internal.application.ToolingApplicationDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.artifact.DeployableArtifactWrapper;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainRepository;
import org.mule.runtime.module.tooling.api.ToolingService;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingServiceBuilder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@code ToolingService}.
 *
 * @since 4.0
 */
public class DefaultToolingService implements ToolingService {

  private static final String MULE_TMP_FILENAME = "tmp";
  private static final String TOOLING_APPS_FOLDER = "tooling";

  private static final String TOOLING_PREFIX = "tooling";
  private static final String APPLICATION = "application";
  private static final String DOMAIN = "domain";

  protected transient final Logger logger = LoggerFactory.getLogger(getClass());

  private final DomainRepository domainRepository;

  private final DefaultDomainFactory domainFactory;
  private final DefaultApplicationFactory applicationFactory;
  private final ToolingApplicationDescriptorFactory applicationDescriptorFactory;

  private File toolingServiceAppsFolder;
  private ArtifactFileWriter artifactFileWriter;

  /**
   * @param domainRepository {@link DomainRepository} to look up for already deployed domains.
   * @param domainFactory factory for creating the {@link Domain}
   * @param applicationFactory factory for creating the {@link Application}
   * @param applicationDescriptorFactory {@link ToolingApplicationDescriptorFactory} to load the application descriptor.
   */
  public DefaultToolingService(DomainRepository domainRepository,
                               DefaultDomainFactory domainFactory,
                               DefaultApplicationFactory applicationFactory,
                               ToolingApplicationDescriptorFactory applicationDescriptorFactory) {
    this.domainRepository = domainRepository;
    this.domainFactory = domainFactory;
    this.applicationFactory = applicationFactory;
    this.applicationDescriptorFactory = applicationDescriptorFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectivityTestingServiceBuilder newConnectivityTestingServiceBuilder() {
    return new DefaultConnectivityTestingServiceBuilder(applicationFactory);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Application createApplication(File applicationLocation) throws IOException {
    File toolingApplicationContent = artifactFileWriter.writeContent(getUniqueIdString(APPLICATION), applicationLocation);
    try {
      return doCreateApplication(toolingApplicationContent, empty());
    } catch (Throwable t) {
      deleteQuietly(toolingApplicationContent);
      throw t;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Application createApplication(File applicationLocation, Optional<Properties> deploymentProperties) throws IOException {
    File toolingApplicationContent = artifactFileWriter.writeContent(getUniqueIdString(APPLICATION), applicationLocation);
    try {
      return doCreateApplication(toolingApplicationContent, deploymentProperties);
    } catch (Throwable t) {
      deleteQuietly(toolingApplicationContent);
      throw t;
    }
  }

  private Application doCreateApplication(File toolingApplicationContent, Optional<Properties> deploymentProperties)
      throws IOException {
    Optional<Properties> mergedDeploymentProperties = of(createDeploymentProperties(deploymentProperties));
    MuleApplicationModel.MuleApplicationModelBuilder applicationArtifactModelBuilder =
        applicationDescriptorFactory.createArtifactModelBuilder(toolingApplicationContent);
    String domainName = mergedDeploymentProperties.get().getProperty(DEPLOYMENT_DOMAIN_NAME_REF);
    if (domainName != null) {
      Domain domain = domainRepository.getDomain(domainName);
      if (domain == null) {
        throw new IllegalArgumentException(format("Domain '%s' is expected to be deployed", domainName));
      }

      MuleArtifactLoaderDescriptor classLoaderModelDescriptorLoader =
          applicationArtifactModelBuilder.getClassLoaderModelDescriptorLoader();
      Map<String, Object> extendedAttributes = new HashMap<>(classLoaderModelDescriptorLoader.getAttributes());
      extendedAttributes.put(CLASSLOADER_MODEL_MAVEN_REACTOR_RESOLVER,
                             new DomainMavenReactorResolver(
                                                            domain
                                                                .getLocation(),
                                                            domain
                                                                .getDescriptor()
                                                                .getBundleDescriptor()));
      applicationArtifactModelBuilder
          .withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptor(classLoaderModelDescriptorLoader.getId(),
                                                                                 extendedAttributes));
      ApplicationDescriptor applicationDescriptor =
          applicationDescriptorFactory.createArtifact(toolingApplicationContent, mergedDeploymentProperties,
                                                      applicationArtifactModelBuilder.build());
      applicationDescriptor.setDomainName(domain.getArtifactName());
      return new ToolingApplicationWrapper(doCreateApplication(applicationDescriptor));
    }
    return new ToolingApplicationWrapper(doCreateApplication(applicationDescriptorFactory.create(toolingApplicationContent,
                                                                                                 mergedDeploymentProperties)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Application createApplication(byte[] appContent) throws IOException {
    File toolingApplicationContent = artifactFileWriter.writeContent(getUniqueIdString(APPLICATION), appContent);
    try {
      return doCreateApplication(toolingApplicationContent, empty());
    } catch (Throwable t) {
      deleteQuietly(toolingApplicationContent);
      throw t;
    }
  }

  private Application doCreateApplication(ApplicationDescriptor applicationDescriptor) throws IOException {
    Application application = applicationFactory.createArtifact(applicationDescriptor);
    application.install();
    application.lazyInit();
    application.start();
    return application;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Domain createDomain(File domainLocation) throws IOException {
    File toolingDomainContent = artifactFileWriter.writeContent(getUniqueIdString(DOMAIN), domainLocation);
    try {
      return doCreateDomain(toolingDomainContent);
    } catch (Throwable t) {
      deleteQuietly(toolingDomainContent);
      throw t;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Domain createDomain(byte[] domainContent) throws IOException {
    File toolingDomainContent = artifactFileWriter.writeContent(getUniqueIdString(DOMAIN), domainContent);
    try {
      return doCreateDomain(toolingDomainContent);
    } catch (Throwable t) {
      deleteQuietly(toolingDomainContent);
      throw t;
    }
  }

  private Domain doCreateDomain(File toolingDomainContent) throws IOException {
    Domain domain = domainFactory.createArtifact(toolingDomainContent, of(createDeploymentProperties()));
    domain.install();
    domain.init();
    //TODO (gfernandes) support lazy init on domains
    //domain.lazyInit();
    domain.start();
    return new ToolingDomainWrapper(domain);
  }

  private static Properties createDeploymentProperties() {
    final Properties properties = new Properties();
    return createDeploymentProperties(of(properties));
  }

  private static Properties createDeploymentProperties(Optional<Properties> deploymentProperties) {
    Properties properties;
    if (deploymentProperties.isPresent()) {
      properties = new Properties();
      properties.putAll(deploymentProperties.get());
    } else {
      properties = new Properties();
    }
    properties.setProperty(MULE_MUTE_APP_LOGS_DEPLOYMENT_PROPERTY, "true");
    return properties;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialise() throws InitialisationException {
    toolingServiceAppsFolder = createToolingServiceAppsFolder();
    artifactFileWriter = new ArtifactFileWriter(toolingServiceAppsFolder);
  }

  /**
   * Creates a folder for this service to upload tooling applications.
   *
   * @return {@link File} working folder.
   * @throws InitialisationException if there was an error while creating the folder.
   */
  private File createToolingServiceAppsFolder() throws InitialisationException {
    File toolingServiceAppsFolder = new File(new File(getMuleBaseFolder(), MULE_TMP_FILENAME), TOOLING_APPS_FOLDER);
    if (!toolingServiceAppsFolder.exists()) {
      boolean folderCreated = toolingServiceAppsFolder.mkdirs();
      if (!folderCreated) {
        throw new InitialisationException(createStaticMessage("Couldn't start up the service"),
                                          new IOException("Couldn't create tooling service resources folder: "
                                              + toolingServiceAppsFolder),
                                          this);
      }
      if (logger.isDebugEnabled()) {
        logger.debug("Create tooling service resources folder at: " + toolingServiceAppsFolder);
      }
    } else {
      try {
        cleanDirectory(toolingServiceAppsFolder);
      } catch (IOException e) {
        logger.warn("Could not clean up tooling service resources folder at: " + toolingServiceAppsFolder);
      }
    }
    return toolingServiceAppsFolder;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() throws MuleException {
    if (toolingServiceAppsFolder != null) {
      try {
        cleanDirectory(toolingServiceAppsFolder);
      } catch (Exception e) {
        logger.warn("Couldn't clean up tooling service resources folder located at: " + toolingServiceAppsFolder);
      }
    }
  }

  /**
   * Generates a unique id for an artifact (domain or application).
   *
   * @return {@link String} a uniqueId
   */
  public String getUniqueIdString(String type) {
    return TOOLING_PREFIX + "-" + type + "-" + UUID.getUUID();
  }

  private class ToolingApplicationWrapper extends ApplicationWrapper {

    protected ToolingApplicationWrapper(Application delegate) throws IOException {
      super(delegate);
    }

    @Override
    public void dispose() {
      File appLocation = this.getLocation();
      try {
        super.dispose();
      } catch (Throwable t) {
        logger.warn(format("Error while disposing application: {} ", this.getArtifactName()), t);
      }
      deleteQuietly(appLocation);
    }

  }

  private class ToolingDomainWrapper extends DeployableArtifactWrapper<Domain, DomainDescriptor> implements Domain {

    protected ToolingDomainWrapper(Domain delegate) throws IOException {
      super(delegate);
    }

    @Override
    public boolean containsSharedResources() {
      return getDelegate().containsSharedResources();
    }

    @Override
    public void dispose() {
      File domainLocation = this.getLocation();
      try {
        super.dispose();
      } catch (Throwable t) {
        logger.warn(format("Error while disposing domain: {} ", super.getArtifactName()), t);
      }
      deleteQuietly(domainLocation);
    }

  }

  public static class DomainMavenReactorResolver implements MavenReactorResolver {

    private File domainArtifactLocation;
    private File domainPomFile;
    private BundleDescriptor domainBundleDescriptor;

    public DomainMavenReactorResolver(File domainArtifactLocation, BundleDescriptor domainBundleDescriptor) {
      this.domainArtifactLocation = domainArtifactLocation;
      this.domainBundleDescriptor = domainBundleDescriptor;
      this.domainPomFile = lookupPomFromMavenLocation(this.domainArtifactLocation);
    }

    @Override
    public File findArtifact(org.mule.maven.client.api.model.BundleDescriptor bundleDescriptor) {
      if (checkArtifact(bundleDescriptor)) {
        if (bundleDescriptor.getType().equals("pom")) {
          return domainPomFile;
        } else {
          return domainArtifactLocation;
        }
      }
      return null;
    }

    @Override
    public List<String> findVersions(org.mule.maven.client.api.model.BundleDescriptor bundleDescriptor) {
      if (checkArtifact(bundleDescriptor)) {
        return singletonList(this.domainBundleDescriptor.getVersion());
      }
      return emptyList();
    }

    private boolean checkArtifact(org.mule.maven.client.api.model.BundleDescriptor bundleDescriptor) {
      return this.domainBundleDescriptor.getGroupId().equals(bundleDescriptor.getGroupId())
          && this.domainBundleDescriptor.getArtifactId().equals(bundleDescriptor.getArtifactId())
          && this.domainBundleDescriptor.getVersion().equals(bundleDescriptor.getVersion());
    }

  }

}
