/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import static java.lang.Boolean.getBoolean;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toSet;
import static org.mule.maven.client.api.model.BundleScope.PROVIDED;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.container.api.MuleFoldersUtil.getMuleHomeFolder;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.MAVEN;
import static org.mule.runtime.module.reboot.MuleContainerBootstrapUtils.isStandalone;
import org.mule.maven.client.api.LocalRepositorySupplierFactory;
import org.mule.maven.client.api.MavenClient;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.core.config.bootstrap.ArtifactType;
import org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants;
import org.mule.runtime.globalconfig.api.GlobalConfigLoader;
import org.mule.runtime.module.artifact.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.descriptor.BundleScope;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModelLoader;
import org.mule.runtime.module.artifact.descriptor.InvalidDescriptorLoaderException;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.maven.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract implementation of {@link ClassLoaderModelLoader} that resolves the dependencies for all the mule artifacts and create
 * the {@link ClassLoaderModel}. It lets the implementations of this class to add artifact's specific class loader URLs
 * 
 * @since 4.0
 */
public abstract class AbstractMavenClassLoaderModelLoader implements ClassLoaderModelLoader {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final LocalRepositorySupplierFactory localRepositorySupplierFactory;
  private MavenClient mavenClient;

  public AbstractMavenClassLoaderModelLoader(MavenClient mavenClient,
                                             LocalRepositorySupplierFactory localRepositorySupplierFactory) {
    this.mavenClient = mavenClient;
    this.localRepositorySupplierFactory = localRepositorySupplierFactory;
  }

  @Override
  public String getId() {
    return MAVEN;
  }

  /**
   * Given an artifact location, it will resolve its dependencies on a Maven based mechanism. It will assume there's a repository
   * folder to look for the artifacts in it (which includes both JAR files as well as POM ones).
   * <p/>
   * It takes care of the transitive compile and runtime dependencies, from which will take the URLs to add them to the resulting
   * {@link ClassLoaderModel}, and it will also consume all Mule plugin dependencies so that further validations can check whether
   * or not all plugins are loaded in memory before running an application.
   * <p/>
   * Finally, it will also tell the resulting {@link ClassLoaderModel} which packages and/or resources has to export, consuming
   * the attributes from the {@link MuleArtifactLoaderDescriptor#getAttributes()} map.
   *
   * @param artifactFile {@link File} where the current plugin to work with.
   * @param attributes a set of attributes to work with, where the current implementation of this class will look for
   *        {@link MavenClassLoaderConstants#EXPORTED_PACKAGES} and {@link MavenClassLoaderConstants#EXPORTED_RESOURCES}
   * @return a {@link ClassLoaderModel} loaded with all its dependencies and URLs.
   */
  @Override
  public final ClassLoaderModel load(File artifactFile, Map<String, Object> attributes, ArtifactType artifactType)
      throws InvalidDescriptorLoaderException {
    return createClassLoaderModel(artifactFile, attributes);
  }

  private ClassLoaderModel createClassLoaderModel(File artifactFile, Map<String, Object> attributes)
      throws InvalidDescriptorLoaderException {

    File containerRepository = null;
    if (isStandalone() && !getBoolean("mule.mode.embedded")) {
      containerRepository = new File(getMuleHomeFolder(), "repository");
      if (!containerRepository.exists()) {
        if (!containerRepository.mkdirs()) {
          // check again since it may have been created already.
          if (!containerRepository.exists()) {
            throw new MuleRuntimeException(I18nMessageFactory
                .createStaticMessage("Failure creating repository folder in MULE_HOME folder "
                    + containerRepository.getAbsolutePath()));
          }
        }
      }
    }

    File localMavenRepositoryLocation = GlobalConfigLoader.getMavenConfig().getLocalMavenRepositoryLocation();

    Supplier<File> compositeRepoLocationSupplier =
        localRepositorySupplierFactory
            .composeSuppliers(localRepositorySupplierFactory.artifactFolderRepositorySupplier(artifactFile,
                                                                                              localMavenRepositoryLocation),
                              localRepositorySupplierFactory.fixedFolderSupplier(localMavenRepositoryLocation));
    File mavenRepository = compositeRepoLocationSupplier.get();
    List<org.mule.maven.client.api.model.BundleDependency> dependencies =
        mavenClient.resolveArtifactDependencies(artifactFile, enabledTestDependencies(), of(mavenRepository),
                                                of(new File(mavenRepository, ".mule")));
    final ClassLoaderModel.ClassLoaderModelBuilder classLoaderModelBuilder = new ClassLoaderModel.ClassLoaderModelBuilder();
    classLoaderModelBuilder
        .exportingPackages(new HashSet<>(getAttribute(attributes, EXPORTED_PACKAGES)))
        .exportingResources(new HashSet<>(getAttribute(attributes, EXPORTED_RESOURCES)));
    Set<BundleDependency> bundleDependencies =
        dependencies.stream().filter(mavenClientDependency -> !mavenClientDependency.getScope().equals(PROVIDED))
            .map(mavenClientDependency -> convertBundleDependency(mavenClientDependency)).collect(toSet());
    loadUrls(artifactFile, classLoaderModelBuilder, bundleDependencies);
    classLoaderModelBuilder.dependingOn(bundleDependencies);
    return classLoaderModelBuilder.build();
  }

  protected BundleDependency convertBundleDependency(org.mule.maven.client.api.model.BundleDependency mavenClientDependency) {
    BundleDependency.Builder builder = new BundleDependency.Builder()
        .setScope(BundleScope.valueOf(mavenClientDependency.getScope().name()))
        .setBundleUrl(mavenClientDependency.getBundleUrl())
        .setDescriptor(convertBundleDescriptor(mavenClientDependency.getDescriptor()));
    return builder.build();
  }

  private BundleDescriptor convertBundleDescriptor(org.mule.maven.client.api.model.BundleDescriptor descriptor) {
    BundleDescriptor.Builder builder = new BundleDescriptor.Builder().setGroupId(descriptor.getGroupId())
        .setArtifactId(descriptor.getArtifactId())
        // Use baseVersion as it will refer to the unresolved meta version (case of SNAPSHOTS instead of timestamp versions)
        .setVersion(descriptor.getBaseVersion())
        .setType(descriptor.getType());
    descriptor.getClassifier().ifPresent(builder::setClassifier);
    return builder.build();
  }

  /**
   * Loads the {@link ClassLoaderModel} from an artifact with the provided maven pom model.
   *
   * @param artifactFile the artifact folder
   * @return a {@link ClassLoaderModel} loaded with all its dependencies and URLs.
   * @throws InvalidDescriptorLoaderException
   */
  public final ClassLoaderModel load(File artifactFile) throws InvalidDescriptorLoaderException {
    return createClassLoaderModel(artifactFile, emptyMap());
  }

  /**
   * Template method to get the unresolved pom model from the artifact file
   *
   * @param artifactFile the artifact file
   * @return the pom model
   */
  protected Model loadPomModel(File artifactFile) {
    return mavenClient.getRawPomModel(artifactFile);
  }

  /**
   * Loads the URLs of the class loader for this artifact.
   * <p>
   * It let's implementations to add artifact specific URLs by letting them override
   * {@link #addArtifactSpecificClassloaderConfiguration(File, ClassLoaderModel.ClassLoaderModelBuilder, Set)}
   * 
   * @param artifactFile the artifact file for which the {@link ClassLoaderModel} is being generated.
   * @param classLoaderModelBuilder the builder of the {@link ClassLoaderModel}
   * @param dependencies the dependencies resolved for this artifact.
   */
  protected void loadUrls(File artifactFile, ClassLoaderModel.ClassLoaderModelBuilder classLoaderModelBuilder,
                          Set<BundleDependency> dependencies) {
    addArtifactSpecificClassloaderConfiguration(artifactFile, classLoaderModelBuilder, dependencies);
    addDependenciesToClasspathUrls(classLoaderModelBuilder, dependencies);
  }

  private void addDependenciesToClasspathUrls(ClassLoaderModel.ClassLoaderModelBuilder classLoaderModelBuilder,
                                              Set<BundleDependency> dependencies) {
    dependencies.stream()
        .filter(dependency -> !MULE_PLUGIN_CLASSIFIER.equals(dependency.getDescriptor().getClassifier().orElse(null)))
        .filter(dependency -> dependency.getBundleUrl() != null)
        .forEach(dependency -> {
          try {
            try {
              classLoaderModelBuilder.containing(dependency.getBundleUrl().toURI().toURL());
            } catch (URISyntaxException e) {
              throw new MuleRuntimeException(e);
            }
          } catch (MalformedURLException e) {
            throw new MuleRuntimeException(e);
          }
        });
  }

  private List<String> getAttribute(Map<String, Object> attributes, String attribute) {
    final Object attributeObject = attributes.getOrDefault(attribute, new ArrayList<String>());
    checkArgument(attributeObject instanceof List, format("The '%s' attribute must be of '%s', found '%s'", attribute,
                                                          List.class.getName(), attributeObject.getClass().getName()));
    return (List<String>) attributeObject;
  }

  /**
   * Template method to enable/disable test dependencies as part of the artifact classpath.
   *
   * @return true if test dependencies must be part of the artifact classpath, false otherwise.
   */
  protected boolean enabledTestDependencies() {
    return false;
  }

  /**
   * Template method to add artifact specific configuration to the
   * {@link org.mule.runtime.module.artifact.descriptor.ClassLoaderModel.ClassLoaderModelBuilder}
   *
   * @param artifactFile the artifact file from which the classloader model is generated.
   * @param classLoaderModelBuilder the builder used to generate {@link ClassLoaderModel} of the artifact.
   * @param dependencies the set of dependencies of the artifact.
   */
  protected void addArtifactSpecificClassloaderConfiguration(File artifactFile,
                                                             ClassLoaderModel.ClassLoaderModelBuilder classLoaderModelBuilder,
                                                             Set<BundleDependency> dependencies) {

  }

}
