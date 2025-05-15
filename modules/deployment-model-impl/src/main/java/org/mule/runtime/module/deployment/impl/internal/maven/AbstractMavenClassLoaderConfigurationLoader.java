/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import static org.mule.maven.client.api.VersionUtils.discoverVersionUtils;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.container.api.MuleFoldersUtil.getMuleHomeFolder;
import static org.mule.runtime.core.internal.util.MuleContainerUtils.getMuleHome;
import static org.mule.runtime.module.artifact.activation.internal.plugin.PluginLocalDependenciesDenylist.isDenylisted;
import static org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor.REPOSITORY_FOLDER;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.INCLUDE_TEST_DEPENDENCIES;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.PRIVILEGED_ARTIFACTS_IDS;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.PRIVILEGED_EXPORTED_PACKAGES;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.BundleScope.PROVIDED;
import static org.mule.tools.api.classloader.ClassLoaderModelJsonSerializer.deserialize;

import static java.lang.Boolean.valueOf;
import static java.lang.String.format;
import static java.nio.file.Files.createTempDirectory;
import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import static com.vdurmont.semver4j.Semver.SemverType.LOOSE;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.maven.client.api.MavenClient;
import org.mule.maven.client.api.MavenReactorResolver;
import org.mule.maven.client.api.VersionUtils;
import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.api.artifact.ArtifactType;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration.ClassLoaderConfigurationBuilder;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfigurationLoader;
import org.mule.runtime.module.artifact.api.descriptor.InvalidDescriptorLoaderException;
import org.mule.runtime.module.artifact.internal.util.FileJarExplorer;
import org.mule.runtime.module.artifact.internal.util.JarExplorer;
import org.mule.runtime.module.artifact.internal.util.JarInfo;
import org.mule.runtime.module.deployment.impl.internal.plugin.MuleArtifactPatchingModel;
import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ClassLoaderModel;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableSet;
import com.vdurmont.semver4j.Semver;

import org.slf4j.Logger;

/**
 * Abstract implementation of {@link ClassLoaderConfigurationLoader} that resolves the dependencies for all the mule artifacts and
 * create the {@link ClassLoaderConfiguration}. It lets the implementations of this class to add artifact's specific class loader
 * URLs
 *
 * @since 4.0
 */
public abstract class AbstractMavenClassLoaderConfigurationLoader implements ClassLoaderConfigurationLoader {

  public static final String CLASSLOADER_MODEL_JSON_DESCRIPTOR = "classloader-model.json";
  public static final String CLASSLOADER_MODEL_JSON_PATCH_DESCRIPTOR = "classloader-model-patch.json";
  public static final String CLASSLOADER_MODEL_JSON_DESCRIPTOR_LOCATION =
      Paths.get("META-INF", "mule-artifact", CLASSLOADER_MODEL_JSON_DESCRIPTOR).toString();
  public static final String CLASSLOADER_MODEL_JSON_PATCH_DESCRIPTOR_LOCATION =
      Paths.get("META-INF", "mule-artifact", CLASSLOADER_MODEL_JSON_PATCH_DESCRIPTOR).toString();
  public static final String MULE_ARTIFACT_PATCHES_LOCATION = Paths.get("lib/patches/mule-artifact-patches").toString();
  public static final String MULE_ARTIFACT_PATCH_JSON_FILE_NAME = "mule-artifact-patch.json";

  public static final String CLASSLOADER_MODEL_MAVEN_REACTOR_RESOLVER = "_classLoaderModelMavenReactorResolver";
  public static final String CLASS_LOADER_MODEL_VERSION_120 = "1.2.0";

  protected final Logger LOGGER = getLogger(this.getClass());

  private final Optional<MavenClient> mavenClient;
  private final Supplier<JarExplorer> jarExplorerFactory;

  public AbstractMavenClassLoaderConfigurationLoader(Optional<MavenClient> mavenClient) {
    this(mavenClient, () -> new FileJarExplorer());
  }

  public AbstractMavenClassLoaderConfigurationLoader(Optional<MavenClient> mavenClient,
                                                     Supplier<JarExplorer> jarExplorerFactory) {
    this.mavenClient = mavenClient;
    this.jarExplorerFactory = jarExplorerFactory;
  }

  @Override
  public String getId() {
    return MULE_LOADER_ID;
  }

  /**
   * Given an artifact location, it will resolve its dependencies on a Maven based mechanism. It will assume there's a repository
   * folder to look for the artifacts in it (which includes both JAR files as well as POM ones).
   * <p/>
   * It takes care of the transitive compile and runtime dependencies, from which will take the URLs to add them to the resulting
   * {@link ClassLoaderConfiguration}, and it will also consume all Mule plugin dependencies so that further validations can check
   * whether or not all plugins are loaded in memory before running an application.
   * <p/>
   * Finally, it will also tell the resulting {@link ClassLoaderConfiguration} which packages and/or resources has to export,
   * consuming the attributes from the {@link MuleArtifactLoaderDescriptor#getAttributes()} map.
   *
   * @param artifactFile {@link File} where the current plugin to work with.
   * @param attributes   a set of attributes to work with, where the current implementation of this class will look for
   *                     {@link ArtifactDescriptorConstants#EXPORTED_PACKAGES} and
   *                     {@link ArtifactDescriptorConstants#EXPORTED_RESOURCES}
   * @return a {@link ClassLoaderConfiguration} loaded with all its dependencies and URLs.
   */
  @Override
  public final ClassLoaderConfiguration load(File artifactFile, Map<String, Object> attributes, ArtifactType artifactType)
      throws InvalidDescriptorLoaderException {
    return createClassLoaderConfiguration(artifactFile, attributes, artifactType);
  }

  protected final ClassLoaderConfiguration createClassLoaderConfiguration(File artifactFile, Map<String, Object> attributes,
                                                                          ArtifactType artifactType)
      throws InvalidDescriptorLoaderException {

    ClassLoaderModel bundleDescriptorMetadata =
        getBundleDescriptorClassLoaderModelMetadata(attributes);
    if (bundleDescriptorMetadata != null) {
      // Avoid parsing the classLoaderModel from JSON again if that is already available from a previous process.
      return createHeavyPackageClassLoaderConfiguration(artifactFile, attributes,
                                                        of(getDeployableArtifactRepositoryFolder(artifactFile)),
                                                        bundleDescriptorMetadata);
    } else if (isHeavyPackage(artifactFile, attributes)) {
      return createHeavyPackageClassLoaderConfiguration(artifactFile, getClassLoaderModelDescriptor(artifactFile), attributes);
    } else {
      return createLightPackageClassLoaderConfiguration(artifactFile, attributes, artifactType,
                                                        mavenClient
                                                            .orElseThrow(() -> new UnsupportedOperationException("A MavenClient must be provided in order to handle lightweight packages.")));
    }
  }

  private ClassLoaderModel getBundleDescriptorClassLoaderModelMetadata(Map<String, Object> attributes) {
    BundleDescriptor bundleDescriptor = (BundleDescriptor) attributes.get(BundleDescriptor.class.getName());

    if (bundleDescriptor == null) {
      return null;
    }

    return (ClassLoaderModel) bundleDescriptor.getMetadata().get(ClassLoaderModel.class.getName());
  }

  private ClassLoaderConfiguration createHeavyPackageClassLoaderConfiguration(File artifactFile, File classLoaderModelDescriptor,
                                                                              Map<String, Object> attributes) {
    return createHeavyPackageClassLoaderConfiguration(artifactFile, classLoaderModelDescriptor, attributes,
                                                      of(getDeployableArtifactRepositoryFolder(artifactFile)));
  }

  protected ClassLoaderConfiguration createHeavyPackageClassLoaderConfiguration(File artifactFile,
                                                                                File classLoaderModelDescriptor,
                                                                                Map<String, Object> attributes,
                                                                                Optional<File> deployableArtifactRepositoryFolder) {
    return createHeavyPackageClassLoaderConfiguration(artifactFile, attributes, deployableArtifactRepositoryFolder,
                                                      getPackagerClassLoaderModel(classLoaderModelDescriptor));
  }

  protected ClassLoaderConfiguration createHeavyPackageClassLoaderConfiguration(File artifactFile, Map<String, Object> attributes,
                                                                                Optional<File> deployableArtifactRepositoryFolder,
                                                                                ClassLoaderModel packagerClassLoaderModel) {
    BundleDescriptor artifactBundleDescriptor = (BundleDescriptor) attributes.get(BundleDescriptor.class.getName());
    final ArtifactClassLoaderConfigurationBuilder classLoaderConfigurationBuilder =
        newHeavyWeightClassLoaderConfigurationBuilder(artifactFile, artifactBundleDescriptor,
                                                      packagerClassLoaderModel, attributes);
    final Set<String> exportedPackages = new HashSet<>(getAttribute(attributes, EXPORTED_PACKAGES));
    final Set<String> exportedResources = new HashSet<>(getAttribute(attributes, EXPORTED_RESOURCES));

    classLoaderConfigurationBuilder
        .exportingPackages(exportedPackages)
        .exportingResources(exportedResources)
        .exportingPrivilegedPackages(new HashSet<>(getAttribute(attributes, PRIVILEGED_EXPORTED_PACKAGES)),
                                     new HashSet<>(getAttribute(attributes, PRIVILEGED_ARTIFACTS_IDS)));

    List<BundleDependency> bundleDependencies;
    if (deployableArtifactRepositoryFolder.isPresent()) {
      List<BundleDependency> patchBundleDependencies =
          getPatchedBundledDependencies(artifactFile, deployableArtifactRepositoryFolder.get());

      bundleDependencies =
          packagerClassLoaderModel.getDependencies().stream().map(artifact -> {
            Optional<BundleDependency> patchedBundledDependency =
                patchBundleDependencies.stream().filter(bundleDependency -> bundleDependency.getDescriptor().getGroupId()
                    .equals(artifact.getArtifactCoordinates().getGroupId()) &&
                    bundleDependency.getDescriptor().getArtifactId().equals(artifact.getArtifactCoordinates().getArtifactId()))
                    .findAny();
            return patchedBundledDependency
                .orElse(createBundleDependencyFromPackagerDependency(getDeployableArtifactRepositoryUriResolver(deployableArtifactRepositoryFolder
                    .get())).apply(artifact));
          }).collect(toList());
    } else {
      bundleDependencies = packagerClassLoaderModel.getDependencies().stream()
          .map(artifact -> createBundleDependencyFromPackagerDependency(uri -> uri).apply(artifact))
          .collect(toList());
    }

    List<URL> patches = getArtifactPatches(artifactBundleDescriptor);

    // This is already filtering out mule-plugin dependencies,
    // since for this case we explicitly need to consume the exported API from the plugin.
    final List<URL> dependenciesArtifactsUrls =
        loadUrls(artifactFile, classLoaderConfigurationBuilder, bundleDependencies, patches);

    ArtifactAttributes artifactAttributes;
    if (new Semver(packagerClassLoaderModel.getVersion(), LOOSE).isLowerThan(CLASS_LOADER_MODEL_VERSION_120)) {
      artifactAttributes = discoverLocalPackages(dependenciesArtifactsUrls);
    } else {
      artifactAttributes = collectLocalPackages(packagerClassLoaderModel);
    }
    if (!isDenylisted(artifactBundleDescriptor)) {
      populateLocalPackages(artifactAttributes, classLoaderConfigurationBuilder);
    }

    classLoaderConfigurationBuilder.dependingOn(new HashSet<>(bundleDependencies));

    return classLoaderConfigurationBuilder.build();
  }

  private ArtifactAttributes collectLocalPackages(ClassLoaderModel packagerClassLoaderModel) {
    ImmutableSet.Builder<String> packagesSetBuilder = ImmutableSet.builder();
    if (packagerClassLoaderModel.getPackages() != null) {
      packagesSetBuilder.add(packagerClassLoaderModel.getPackages());
    }

    ImmutableSet.Builder<String> resourcesSetBuilder = ImmutableSet.builder();
    if (packagerClassLoaderModel.getResources() != null) {
      resourcesSetBuilder.add(packagerClassLoaderModel.getResources());
    }

    packagerClassLoaderModel.getDependencies().stream().forEach(artifact -> {
      if (artifact.getPackages() != null) {
        packagesSetBuilder.add(artifact.getPackages());
      }
      if (artifact.getResources() != null) {
        resourcesSetBuilder.add(artifact.getResources());
      }
    });

    return new ArtifactAttributes(packagesSetBuilder.build(), resourcesSetBuilder.build());
  }

  private List<URL> getArtifactPatches(BundleDescriptor artifactBundleDescriptor) {
    List<URL> patches = new ArrayList<>();
    ArtifactCoordinates thisArtifactCoordinates = artifactBundleDescriptor;
    String artifactId = thisArtifactCoordinates.getGroupId() + ":"
        + thisArtifactCoordinates.getArtifactId() + ":" + thisArtifactCoordinates.getVersion();
    try {
      File muleArtifactPatchesFolder = new File(getMuleHome(), MULE_ARTIFACT_PATCHES_LOCATION);
      if (muleArtifactPatchesFolder.exists()) {
        final VersionUtils versionUtils = discoverVersionUtils(this.getClass().getClassLoader());

        String[] jarFiles = muleArtifactPatchesFolder.list((dir, name) -> name != null && name.endsWith(".jar"));
        for (String jarFile : jarFiles) {
          MuleArtifactPatchingModel muleArtifactPatchingModel =
              MuleArtifactPatchingModel.loadModel(new File(muleArtifactPatchesFolder, jarFile));
          ArtifactCoordinates patchedArtifactCoordinates = muleArtifactPatchingModel.getArtifactCoordinates();
          if (patchedArtifactCoordinates.getGroupId().equals(thisArtifactCoordinates.getGroupId()) &&
              patchedArtifactCoordinates.getArtifactId().equals(thisArtifactCoordinates.getArtifactId()) &&
              patchedArtifactCoordinates.getClassifier().equals(thisArtifactCoordinates.getClassifier())) {

            boolean versionContained;
            try {
              versionContained = versionUtils
                  .containsVersion(thisArtifactCoordinates.getVersion(), muleArtifactPatchingModel.getAffectedVersions());
            } catch (IllegalArgumentException e) {
              LOGGER.warn(e.getMessage() + ", patches against this artifact will not be applied");
              return emptyList();
            }

            if (versionContained) {
              try {
                URL mulePluginPatchUrl =
                    new File(getMuleHome(),
                             Paths.get(MULE_ARTIFACT_PATCHES_LOCATION, jarFile).toString())
                                 .toURL();
                patches.add(mulePluginPatchUrl);
                LOGGER.info(String.format("Patching artifact %s with patch file %s", artifactId, jarFile));
              } catch (MalformedURLException e) {
                throw new MuleRuntimeException(e);
              }
            }
          }
        }
      }
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage(format("There was an error processing the patches in %s file for artifact %s",
                                                                MULE_ARTIFACT_PATCHES_LOCATION, artifactId)),
                                     e);
    }
    return patches;
  }

  /**
   * Template method to deserialize a {@code classloader-model.json} into the expected {@link ClassLoaderModel} implementation
   *
   * @param classLoaderModelDescriptor
   * @return a {@link ClassLoaderModel}
   */
  protected abstract ClassLoaderModel getPackagerClassLoaderModel(File classLoaderModelDescriptor);

  private List<BundleDependency> getPatchedBundledDependencies(File artifactFile, File deployableArtifactRepositoryFolder) {
    List<BundleDependency> patchBundleDependencies = new ArrayList<>();
    File classLoaderModelPatchDescriptor = getClassLoaderModelPatchDescriptor(artifactFile);
    if (classLoaderModelPatchDescriptor.exists()) {
      ClassLoaderModel packagerClassLoaderModelPatch =
          deserialize(classLoaderModelPatchDescriptor);
      patchBundleDependencies.addAll(packagerClassLoaderModelPatch.getDependencies().stream()
          .map(artifact -> createBundleDependencyFromPackagerDependency(getDeployableArtifactRepositoryUriResolver(deployableArtifactRepositoryFolder))
              .apply(artifact))
          .collect(toList()));
    }
    return patchBundleDependencies;
  }

  private Function<URI, URI> getDeployableArtifactRepositoryUriResolver(File deployableArtifactRepositoryFolder) {
    return uri -> new File(deployableArtifactRepositoryFolder, uri.toString()).toURI();
  }

  private File getDeployableArtifactRepositoryFolder(File artifactFile) {
    if (artifactFile.isDirectory()) {
      return artifactFile;
    }

    return findRepositoryFolder(artifactFile).getParentFile();
  }

  private File findRepositoryFolder(File artifactFile) {
    while (!getMuleHomeFolder().equals(artifactFile) && !REPOSITORY_FOLDER.equals(artifactFile.getName())) {
      artifactFile = artifactFile.getParentFile();
    }

    if (!REPOSITORY_FOLDER.equals(artifactFile.getName()) || !artifactFile.isDirectory()) {
      throw new IllegalStateException("Unable to find repository folder for artifact " + artifactFile.getAbsolutePath());
    }

    return artifactFile;
  }

  private Function<Artifact, BundleDependency> createBundleDependencyFromPackagerDependency(Function<URI, URI> uriResolver) {
    return d -> {
      URI bundle = d.getUri();
      if (!d.getUri().isAbsolute()) {
        bundle = uriResolver.apply(d.getUri());
      }

      return new BundleDependency.Builder()
          .setDescriptor(
                         new BundleDescriptor.Builder().setArtifactId(d.getArtifactCoordinates().getArtifactId())
                             .setGroupId(d.getArtifactCoordinates().getGroupId())
                             .setClassifier(d.getArtifactCoordinates().getClassifier())
                             .setType(d.getArtifactCoordinates().getType())
                             .setVersion(d.getArtifactCoordinates().getVersion())
                             .setBaseVersion(d.getArtifactCoordinates().getVersion())
                             .build())
          .setBundleUri(bundle)
          .build();
    };
  }

  protected boolean isHeavyPackage(File artifactFile, Map<String, Object> attributes) {
    return getClassLoaderModelDescriptor(artifactFile).exists();
  }

  protected File getClassLoaderModelDescriptor(File artifactFile) {
    return new File(artifactFile, CLASSLOADER_MODEL_JSON_DESCRIPTOR_LOCATION);
  }

  protected File getClassLoaderModelPatchDescriptor(File artifactFile) {
    return new File(artifactFile, CLASSLOADER_MODEL_JSON_PATCH_DESCRIPTOR_LOCATION);
  }

  protected List<BundleDependency> resolveArtifactDependencies(File artifactFile, Map<String, Object> attributes,
                                                               ArtifactType artifactType, MavenClient mavenClient) {
    Optional<File> mavenRepository = getLocalMavenRepo(artifactFile, mavenClient);

    boolean includeProvidedDependencies = includeProvidedDependencies(artifactType);
    Optional<MavenReactorResolver> mavenReactorResolver = ofNullable((MavenReactorResolver) attributes
        .get(CLASSLOADER_MODEL_MAVEN_REACTOR_RESOLVER));
    Optional<File> temporaryDirectory;
    try {
      temporaryDirectory = of(createTempDirectory(null).toFile());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create directory", e);
    }
    try {
      List<org.mule.maven.pom.parser.api.model.BundleDependency> dependencies =
          mavenClient.resolveArtifactDependencies(artifactFile, includeTestDependencies(attributes),
                                                  includeProvidedDependencies, mavenRepository,
                                                  mavenReactorResolver,
                                                  temporaryDirectory);
      DependencyConverter dependencyConverter = new DependencyConverter();
      return dependencies.stream().map(dependencyConverter::convert).collect(toList());
    } finally {
      deleteQuietly(temporaryDirectory.get());
    }
  }

  protected final Optional<File> getLocalMavenRepo(File artifactFile, MavenClient mavenClient) {
    Optional<File> mavenRepository = ofNullable(mavenClient.getMavenConfiguration().getLocalMavenRepositoryLocation());
    if (!mavenRepository.isPresent()) {
      throw new MuleRuntimeException(createStaticMessage(
                                                         format("Missing Maven local repository configuration while trying to resolve class loader configuration for lightweight artifact: %s",
                                                                artifactFile.getName())));
    }
    return mavenRepository;
  }

  protected ClassLoaderConfiguration createLightPackageClassLoaderConfiguration(File artifactFile,
                                                                                Map<String, Object> attributes,
                                                                                ArtifactType artifactType,
                                                                                MavenClient mavenClient) {
    List<BundleDependency> resolvedDependencies =
        resolveArtifactDependencies(artifactFile, attributes, artifactType, mavenClient);

    List<BundleDependency> nonProvidedDependencies =
        resolvedDependencies.stream().filter(dep -> !PROVIDED.equals(dep.getScope())).collect(toList());

    BundleDescriptor artifactBundleDescriptor = (BundleDescriptor) attributes.get(BundleDescriptor.class.getName());
    final LightweightClassLoaderConfigurationBuilder classLoaderConfigurationBuilder =
        newLightweightClassLoaderConfigurationBuilder(artifactFile, artifactBundleDescriptor,
                                                      mavenClient, attributes, nonProvidedDependencies);

    final Set<String> exportedPackages = new HashSet<>(getAttribute(attributes, EXPORTED_PACKAGES));
    final Set<String> exportedResources = new HashSet<>(getAttribute(attributes, EXPORTED_RESOURCES));

    classLoaderConfigurationBuilder
        .exportingPackages(exportedPackages)
        .exportingResources(exportedResources)
        .exportingPrivilegedPackages(new HashSet<>(getAttribute(attributes, PRIVILEGED_EXPORTED_PACKAGES)),
                                     new HashSet<>(getAttribute(attributes, PRIVILEGED_ARTIFACTS_IDS)))
        .includeTestDependencies(valueOf(getSimpleAttribute(attributes, INCLUDE_TEST_DEPENDENCIES, "false")));

    // This is already filtering out mule-plugin dependencies,
    // since for this case we explicitly need to consume the exported API from the plugin.
    final List<URL> dependenciesArtifactsUrls =
        loadUrls(artifactFile, classLoaderConfigurationBuilder, nonProvidedDependencies, emptyList());

    if (!isDenylisted(artifactBundleDescriptor)) {
      populateLocalPackages(discoverLocalPackages(dependenciesArtifactsUrls), classLoaderConfigurationBuilder);
    }

    classLoaderConfigurationBuilder.dependingOn(new HashSet<>(resolvedDependencies));

    return classLoaderConfigurationBuilder.build();
  }

  protected abstract LightweightClassLoaderConfigurationBuilder newLightweightClassLoaderConfigurationBuilder(File artifactFile,
                                                                                                              BundleDescriptor bundleDescriptor,
                                                                                                              MavenClient mavenClient,
                                                                                                              Map<String, Object> attributes,
                                                                                                              List<BundleDependency> nonProvidedDependencies);

  protected abstract HeavyweightClassLoaderConfigurationBuilder newHeavyWeightClassLoaderConfigurationBuilder(File artifactFile,
                                                                                                              BundleDescriptor artifactBundleDescriptor,
                                                                                                              ClassLoaderModel packagerClassLoaderModel,
                                                                                                              Map<String, Object> attributes);

  private ArtifactAttributes discoverLocalPackages(List<URL> dependenciesArtifactsUrls) {
    final Set<String> packages = new HashSet<>();
    final Set<String> resources = new HashSet<>();

    for (URL dependencyArtifactUrl : dependenciesArtifactsUrls) {
      final URI dependencyArtifactUri;
      try {
        dependencyArtifactUri = dependencyArtifactUrl.toURI();
      } catch (URISyntaxException e) {
        throw new MuleRuntimeException(e);
      }

      try {
        final JarInfo exploredJar = jarExplorerFactory.get().explore(dependencyArtifactUri);
        packages.addAll(exploredJar.getPackages());
        resources.addAll(exploredJar.getResources());
      } catch (IllegalArgumentException e) {
        // Workaround for MMP-499
        LOGGER.warn("File for dependency artifact not found: '{}'. Skipped localPackages scanning for that artifact.",
                    dependencyArtifactUri);
      }
    }
    return new ArtifactAttributes(packages, resources);
  }

  protected void populateLocalPackages(ArtifactAttributes artifactAttributes,
                                       ArtifactClassLoaderConfigurationBuilder classLoaderConfigurationBuilder) {
    classLoaderConfigurationBuilder.withLocalPackages(new HashSet<>(artifactAttributes.getPackages()));
    classLoaderConfigurationBuilder.withLocalResources(new HashSet<>(artifactAttributes.getResources()));
  }

  protected abstract boolean includeProvidedDependencies(ArtifactType artifactType);

  /**
   * Loads the URLs of the class loader for this artifact.
   * <p>
   * It let's implementations to add artifact specific URLs by letting them override
   * {@link #addArtifactSpecificClassloaderConfiguration(ArtifactClassLoaderConfigurationBuilder)}
   *
   * @param artifactFile                    the artifact file for which the {@link ClassLoaderConfiguration} is being generated.
   * @param classLoaderConfigurationBuilder the builder of the {@link ClassLoaderConfiguration}.
   * @param dependencies                    the dependencies resolved for this artifact.
   * @param patches
   */
  private List<URL> loadUrls(File artifactFile, ArtifactClassLoaderConfigurationBuilder classLoaderConfigurationBuilder,
                             List<BundleDependency> dependencies, List<URL> patches) {
    for (URL patchUrl : patches) {
      classLoaderConfigurationBuilder.containing(patchUrl);
    }

    final List<URL> dependenciesArtifactsUrls = new ArrayList<>();

    final URL artifactFileUrl = getUrl(artifactFile, artifactFile);
    classLoaderConfigurationBuilder.containing(artifactFileUrl);
    dependenciesArtifactsUrls.add(artifactFileUrl);

    dependenciesArtifactsUrls.addAll(addArtifactSpecificClassloaderConfiguration(classLoaderConfigurationBuilder));
    dependenciesArtifactsUrls.addAll(addDependenciesToClasspathUrls(artifactFile, classLoaderConfigurationBuilder, dependencies));

    return dependenciesArtifactsUrls;
  }

  private URL getUrl(File artifactFile, File file) {
    try {
      return file.toURI().toURL();
    } catch (MalformedURLException e) {
      throw new ArtifactDescriptorCreateException(format("There was an exception obtaining the URL for the artifact [%s], file [%s]",
                                                         artifactFile.getAbsolutePath(), file.getAbsolutePath()),
                                                  e);
    }
  }

  private List<URL> addDependenciesToClasspathUrls(File artifactFile,
                                                   ClassLoaderConfigurationBuilder classLoaderConfigurationBuilder,
                                                   List<BundleDependency> dependencies) {
    final List<URL> dependenciesArtifactsUrls = new ArrayList<>();

    dependencies.stream()
        .filter(dependency -> !MULE_PLUGIN_CLASSIFIER.equals(dependency.getDescriptor().getClassifier().orElse(null)))
        .filter(dependency -> dependency.getBundleUri() != null)
        .filter(dependency -> !validateMuleRuntimeSharedLibrary(dependency.getDescriptor().getGroupId(),
                                                                dependency.getDescriptor().getArtifactId(),
                                                                artifactFile.getName()))
        .forEach(dependency -> {
          final URL dependencyArtifactUrl;
          try {
            dependencyArtifactUrl = dependency.getBundleUri().toURL();
          } catch (MalformedURLException e) {
            throw new MuleRuntimeException(e);
          }
          classLoaderConfigurationBuilder.containing(dependencyArtifactUrl);
          dependenciesArtifactsUrls.add(dependencyArtifactUrl);
        });

    return dependenciesArtifactsUrls;
  }

  protected final boolean validateMuleRuntimeSharedLibrary(String groupId, String artifactId, String artifactFileName) {
    if ("org.mule.runtime".equals(groupId)
        || "com.mulesoft.mule.runtime.modules".equals(groupId)) {
      LOGGER
          .warn("Internal plugin library '{}:{}' is a Mule Runtime dependency. It will not be used by '{}' in order to avoid classloading issues. Please consider removing it, or at least not putting it with 'compile' scope.",
                groupId, artifactId, artifactFileName);
      return true;
    } else {
      return false;
    }
  }

  private List<String> getAttribute(Map<String, Object> attributes, String attribute) {
    final Object attributeObject = attributes.getOrDefault(attribute, new ArrayList<>());
    checkArgument(attributeObject instanceof List, format("The '%s' attribute must be of '%s', found '%s'", attribute,
                                                          List.class.getName(), attributeObject.getClass().getName()));
    return (List<String>) attributeObject;
  }

  private <T> T getSimpleAttribute(Map<String, Object> attributes, String attribute, T defaultValue) {
    return (T) attributes.getOrDefault(attribute, defaultValue);
  }

  /**
   * Template method to enable/disable test dependencies as part of the artifact classpath.
   *
   * @return true if test dependencies must be part of the artifact classpath, false otherwise.
   */
  protected boolean includeTestDependencies(Map<String, Object> attributes) {
    return false;
  }

  /**
   * Template method to add artifact specific configuration to the {@link ClassLoaderConfigurationBuilder}
   *
   * @param classLoaderConfigurationBuilder the builder used to generate {@link ClassLoaderConfiguration} of the artifact.
   */
  protected List<URL> addArtifactSpecificClassloaderConfiguration(ArtifactClassLoaderConfigurationBuilder classLoaderConfigurationBuilder) {
    return emptyList();
  }
}
