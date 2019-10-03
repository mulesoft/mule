/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.plugin;

import static com.google.common.io.Files.createTempDir;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.PLUGIN;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.META_INF;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor.MULE_ARTIFACT;
import static org.mule.runtime.module.artifact.api.descriptor.BundleScope.SYSTEM;
import static org.mule.tools.api.classloader.ClassLoaderModelJsonSerializer.deserialize;

import org.mule.maven.client.api.MavenClient;
import org.mule.maven.client.api.MavenReactorResolver;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.api.descriptor.InvalidDescriptorLoaderException;
import org.mule.runtime.module.deployment.impl.internal.maven.AbstractMavenClassLoaderModelLoader;
import org.mule.runtime.module.deployment.impl.internal.maven.ArtifactClassLoaderModelBuilder;
import org.mule.runtime.module.deployment.impl.internal.maven.DependencyConverter;
import org.mule.runtime.module.deployment.impl.internal.maven.HeavyweightClassLoaderModelBuilder;
import org.mule.runtime.module.deployment.impl.internal.maven.LightweightClassLoaderModelBuilder;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import org.apache.maven.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible of returning the {@link BundleDescriptor} of a given plugin's location and also creating a
 * {@link ClassLoaderModel}
 *
 * @since 4.0
 */
public class PluginMavenClassLoaderModelLoader extends AbstractMavenClassLoaderModelLoader {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  private static final String JAR = "jar";
  private static final String POM = "pom";

  public PluginMavenClassLoaderModelLoader(MavenClient mavenClient) {
    super(mavenClient);
  }

  @Override
  protected ClassLoaderModel createClassLoaderModel(File artifactFile, Map<String, Object> attributes, ArtifactType artifactType)
      throws InvalidDescriptorLoaderException {
    if (super.isHeavyPackage(artifactFile, attributes)) {
      return super.createClassLoaderModel(artifactFile, attributes, artifactType);
    }

    // If it is a lightweight which uses the local repository a class-loader-model.json may be present in the META-INF/mule-artifact
    if (attributes instanceof PluginExtendedClassLoaderModelAttributes) {
      PluginExtendedClassLoaderModelAttributes pluginExtendedClassLoaderModelAttributes =
          (PluginExtendedClassLoaderModelAttributes) attributes;
      BundleDescriptor pluginBundleDescriptor =
          (BundleDescriptor) pluginExtendedClassLoaderModelAttributes.get(BundleDescriptor.class.getName());
      File rootFolder = pluginExtendedClassLoaderModelAttributes.getDeployableArtifactDescriptor().getRootFolder();
      // mule-plugin has been found as a dependency from another mule-plugin and not present in the deployable dependency graph (system scope dependencies)
      if (rootFolder != null) {
        File muleArtifactJson =
            new File(rootFolder.getAbsolutePath(), getPathForMuleArtifactJson(pluginBundleDescriptor));
        if (muleArtifactJson.exists()) {
          return createHeavyPackageClassLoaderModel(artifactFile, muleArtifactJson, attributes, empty());
        }
      }
    }
    return createLightPackageClassLoaderModel(artifactFile, attributes, artifactType);
  }

  private String getPathForMuleArtifactJson(BundleDescriptor pluginBundleDescriptor) {
    StringBuilder path = new StringBuilder(128);
    char slashChar = '/';
    path.append(META_INF).append(slashChar);
    path.append(MULE_ARTIFACT).append(slashChar);
    path.append(pluginBundleDescriptor.getGroupId().replace('.', slashChar)).append(slashChar);
    path.append(pluginBundleDescriptor.getArtifactId()).append(slashChar);
    path.append(pluginBundleDescriptor.getBaseVersion()).append(slashChar);
    path.append(CLASSLOADER_MODEL_JSON_DESCRIPTOR);
    return path.toString();
  }

  @Override
  public String getId() {
    return MULE_LOADER_ID;
  }

  @Override
  protected List<URL> addArtifactSpecificClassloaderConfiguration(ArtifactClassLoaderModelBuilder classLoaderModelBuilder) {
    return classLoaderModelBuilder.includeAdditionalPluginDependencies();
  }

  @Override
  public boolean supportsArtifactType(ArtifactType artifactType) {
    return artifactType.equals(PLUGIN);
  }

  @Override
  protected boolean includeProvidedDependencies(ArtifactType artifactType) {
    return false;
  }

  @Override
  protected File getClassLoaderModelDescriptor(File artifactFile) {
    return new File(artifactFile.getParent(), CLASSLOADER_MODEL_JSON_DESCRIPTOR);
  }

  @Override
  protected LightweightClassLoaderModelBuilder newLightweightClassLoaderModelBuilder(File artifactFile,
                                                                                     BundleDescriptor artifactBundleDescriptor,
                                                                                     MavenClient mavenClient,
                                                                                     Map<String, Object> attributes,
                                                                                     List<BundleDependency> nonProvidedDependencies) {
    final LightweightClassLoaderModelBuilder lightweightClassLoaderModelBuilder =
        new LightweightClassLoaderModelBuilder(artifactFile, artifactBundleDescriptor, mavenClient, nonProvidedDependencies);
    configClassLoaderModelBuilder(lightweightClassLoaderModelBuilder, attributes);
    return lightweightClassLoaderModelBuilder;
  }

  @Override
  protected HeavyweightClassLoaderModelBuilder newHeavyWeightClassLoaderModelBuilder(File artifactFile,
                                                                                     BundleDescriptor artifactBundleDescriptor,
                                                                                     org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel,
                                                                                     Map<String, Object> attributes) {
    final HeavyweightClassLoaderModelBuilder heavyweightClassLoaderModelBuilder =
        new HeavyweightClassLoaderModelBuilder(artifactFile, artifactBundleDescriptor, packagerClassLoaderModel);
    configClassLoaderModelBuilder(heavyweightClassLoaderModelBuilder, attributes);
    return heavyweightClassLoaderModelBuilder;
  }

  @Override
  protected List<BundleDependency> resolveArtifactDependencies(File artifactFile, Map<String, Object> attributes,
                                                               ArtifactType artifactType) {
    if (attributes instanceof PluginExtendedClassLoaderModelAttributes) {
      BundleDescriptor pluginBundleDescriptor = (BundleDescriptor) attributes.get(BundleDescriptor.class.getName());
      ArtifactDescriptor deployableArtifactDescriptor =
          ((PluginExtendedClassLoaderModelAttributes) attributes).getDeployableArtifactDescriptor();
      Set<BundleDependency> deployableArtifactDescriptorDependencies =
          deployableArtifactDescriptor.getClassLoaderModel().getDependencies();
      BundleDependency pluginDependencyInDeployableArtifact = deployableArtifactDescriptorDependencies.stream()
          .filter(dep -> dep.getDescriptor().equals(pluginBundleDescriptor)).findFirst()
          .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Could not find required descriptor. Looking for: "
              + pluginBundleDescriptor + " in " + deployableArtifactDescriptorDependencies)));

      // MTF/MUnit declares the mule-plugin being tested as system scope therefore its transitive dependencies
      // will not be included in the dependency graph of the deployable artifact and we need to use Mule Maven Client
      // to resolve its dependencies
      if (!SYSTEM.equals(pluginDependencyInDeployableArtifact.getScope())) {
        return collectTransitiveDependencies(pluginDependencyInDeployableArtifact);
      } else {
        if (logger.isWarnEnabled()) {
          logger.warn(format(
                             "Resolving a mule-plugin '%s' with system scope in order to resolve its class loader model. Dependency resolution may fail due to remote repositories from the deployable artifact will not be considered. Prevent this by using compile scope instead",
                             pluginDependencyInDeployableArtifact.getDescriptor()));
        }
      }
    }
    // Backward compatible resolution for resolving dependencies for a mule-plugin with Mule Maven Client
    return resolveArtifactDependenciesUsingMavenClient(artifactFile);
  }

  private List<BundleDependency> resolveArtifactDependenciesUsingMavenClient(File artifactFile) {
    if (logger.isWarnEnabled()) {
      logger.warn(format(
                         "Resolving a mule-plugin from '%s' without the deployable resolution context in order to resolve its class loader model. "
                             +
                             "Dependency resolution may fail due to remote repositories from the deployable artifact will not be considered",
                         artifactFile));
    }

    try (MuleSystemPluginMavenReactorResolver reactor =
        new MuleSystemPluginMavenReactorResolver(artifactFile)) {

      Optional<File> mavenRepository = ofNullable(mavenClient.getMavenConfiguration().getLocalMavenRepositoryLocation());
      if (!mavenRepository.isPresent()) {
        throw new MuleRuntimeException(createStaticMessage(
                                                           format("Missing Maven local repository configuration while trying to resolve class loader model for lightweight artifact: %s",
                                                                  artifactFile.getName())));
      }

      // reactor to resolve the mule-plugin pom and jar file from the location provided in the system dependency.
      org.mule.maven.client.api.model.BundleDescriptor mavenClientBundleDescriptor =
          new org.mule.maven.client.api.model.BundleDescriptor.Builder()
              .setGroupId(reactor.getEffectiveModel().getGroupId())
              .setArtifactId(reactor.getEffectiveModel().getArtifactId())
              .setVersion(reactor.getEffectiveModel().getVersion())
              .setClassifier(MULE_PLUGIN_CLASSIFIER)
              .setType(JAR)
              .build();
      // It will collect the dependencies of the mule-plugin following the same rules that we have when it is declared
      // as compile in a mule-application or mule-domain, without provided and test scope dependencies.
      List<org.mule.maven.client.api.model.BundleDependency> dependencies =
          mavenClient.resolveArtifactDependencies(ImmutableList.of(mavenClientBundleDescriptor),
                                                  mavenRepository,
                                                  of(reactor));
      // The result will only have one dependency and that dependency will be same mule-plugin with its transitive dependencies.
      return collectTransitiveDependencies(new DependencyConverter().convert(dependencies.get(0)));
    }
  }

  private class MuleSystemPluginMavenReactorResolver implements MavenReactorResolver, AutoCloseable {

    private final File temporaryFolder = createTempDir();

    private final Model effectiveModel;

    private final File pomFile;
    private final File artifactFile;

    public MuleSystemPluginMavenReactorResolver(File artifactFile) {
      this.effectiveModel = mavenClient.getEffectiveModel(artifactFile, of(temporaryFolder));

      this.pomFile = effectiveModel.getPomFile();
      this.artifactFile = artifactFile;
    }

    public Model getEffectiveModel() {
      return effectiveModel;
    }

    @Override
    public File findArtifact(org.mule.maven.client.api.model.BundleDescriptor bundleDescriptor) {
      if (checkArtifact(bundleDescriptor)) {
        if (bundleDescriptor.getType().equals(POM)) {
          return pomFile;
        } else {
          return artifactFile;
        }
      }
      return null;
    }

    @Override
    public List<String> findVersions(org.mule.maven.client.api.model.BundleDescriptor bundleDescriptor) {
      if (checkArtifact(bundleDescriptor)) {
        return ImmutableList.of(this.effectiveModel.getVersion());
      }
      return emptyList();
    }

    private boolean checkArtifact(org.mule.maven.client.api.model.BundleDescriptor bundleDescriptor) {
      return this.effectiveModel.getGroupId().equals(bundleDescriptor.getGroupId())
          && this.effectiveModel.getArtifactId().equals(bundleDescriptor.getArtifactId())
          && this.effectiveModel.getVersion().equals(bundleDescriptor.getVersion());
    }

    @Override
    public void close() {
      deleteQuietly(temporaryFolder);
    }

  }

  private List<BundleDependency> collectTransitiveDependencies(BundleDependency rootDependency) {
    List<BundleDependency> allTransitiveDependencies = new ArrayList<>();
    for (BundleDependency transitiveDependency : rootDependency.getTransitiveDependenciesList()) {
      allTransitiveDependencies.add(transitiveDependency);
      if (transitiveDependency.getDescriptor().getClassifier().map(c -> !MULE_PLUGIN_CLASSIFIER.equals(c)).orElse(true)) {
        allTransitiveDependencies.addAll(collectTransitiveDependencies(transitiveDependency));
      }
    }
    return allTransitiveDependencies;
  }

  private void configClassLoaderModelBuilder(ArtifactClassLoaderModelBuilder classLoaderModelBuilder,
                                             Map<String, Object> attributes) {
    if (attributes instanceof PluginExtendedClassLoaderModelAttributes) {
      classLoaderModelBuilder.setDeployableArtifactDescriptor(((PluginExtendedClassLoaderModelAttributes) attributes)
          .getDeployableArtifactDescriptor());
    }
  }

  @Override
  protected org.mule.tools.api.classloader.model.ClassLoaderModel getPackagerClassLoaderModel(File classLoaderModelDescriptor) {
    return deserialize(classLoaderModelDescriptor);
  }
}
