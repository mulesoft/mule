/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import static com.google.common.io.Files.createTempDir;
import static java.lang.Boolean.valueOf;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.container.api.MuleFoldersUtil.getMuleHomeFolder;
import static org.mule.runtime.deployment.model.api.application.ApplicationDescriptor.REPOSITORY_FOLDER;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.INCLUDE_TEST_DEPENDENCIES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.PRIVILEGED_ARTIFACTS_IDS;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.PRIVILEGED_EXPORTED_PACKAGES;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.BundleScope.PROVIDED;
import static org.mule.tools.api.classloader.ClassLoaderModelJsonSerializer.deserialize;
import org.mule.maven.client.api.MavenClient;
import org.mule.maven.client.api.MavenReactorResolver;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModelLoader;
import org.mule.runtime.module.artifact.api.descriptor.InvalidDescriptorLoaderException;
import org.mule.runtime.module.artifact.internal.util.FileJarExplorer;
import org.mule.runtime.module.artifact.internal.util.JarExplorer;
import org.mule.runtime.module.artifact.internal.util.JarInfo;
import org.mule.runtime.module.reboot.api.MuleContainerBootstrapUtils;
import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;

import com.google.common.io.PatternFilenameFilter;

import java.io.File;
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
import java.util.stream.Collectors;

import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract implementation of {@link ClassLoaderModelLoader} that resolves the dependencies for all the mule artifacts and create
 * the {@link ClassLoaderModel}. It lets the implementations of this class to add artifact's specific class loader URLs
 *
 * @since 4.0
 */
public abstract class AbstractMavenClassLoaderModelLoader implements ClassLoaderModelLoader {

  public static final String CLASSLOADER_MODEL_JSON_DESCRIPTOR = "classloader-model.json";
  public static final String CLASSLOADER_MODEL_JSON_PATCH_DESCRIPTOR = "classloader-model-patch.json";
  public static final String CLASSLOADER_MODEL_JSON_DESCRIPTOR_LOCATION =
      Paths.get("META-INF", "mule-artifact", CLASSLOADER_MODEL_JSON_DESCRIPTOR).toString();
  public static final String CLASSLOADER_MODEL_JSON_PATCH_DESCRIPTOR_LOCATION =
      Paths.get("META-INF", "mule-artifact", CLASSLOADER_MODEL_JSON_PATCH_DESCRIPTOR).toString();
  public static final String MULE_ARTIFACT_PATCHES_LOCATION = Paths.get("lib/patches/mule-artifact-patches").toString();
  public static final String MULE_ARTIFACT_PATCH_JSON_FILE_NAME = "mule-artifact-patch.json";
  public static final String MULE_ARTIFACT_PATCHES_JSON_LOCATION =
      Paths.get(MULE_ARTIFACT_PATCHES_LOCATION, MULE_ARTIFACT_PATCH_JSON_FILE_NAME).toString();

  public static final String CLASSLOADER_MODEL_MAVEN_REACTOR_RESOLVER = "_classLoaderModelMavenReactorResolver";

  protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
  protected MavenClient mavenClient;
  private final Supplier<JarExplorer> jarExplorerFactory;

  public AbstractMavenClassLoaderModelLoader(MavenClient mavenClient) {
    this(mavenClient, () -> new FileJarExplorer());
  }

  public AbstractMavenClassLoaderModelLoader(MavenClient mavenClient, Supplier<JarExplorer> jarExplorerFactory) {
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
   * {@link ClassLoaderModel}, and it will also consume all Mule plugin dependencies so that further validations can check whether
   * or not all plugins are loaded in memory before running an application.
   * <p/>
   * Finally, it will also tell the resulting {@link ClassLoaderModel} which packages and/or resources has to export, consuming
   * the attributes from the {@link MuleArtifactLoaderDescriptor#getAttributes()} map.
   *
   * @param artifactFile {@link File} where the current plugin to work with.
   * @param attributes   a set of attributes to work with, where the current implementation of this class will look for
   *                     {@link ArtifactDescriptorConstants#EXPORTED_PACKAGES} and {@link ArtifactDescriptorConstants#EXPORTED_RESOURCES}
   * @return a {@link ClassLoaderModel} loaded with all its dependencies and URLs.
   */
  @Override
  public final ClassLoaderModel load(File artifactFile, Map<String, Object> attributes, ArtifactType artifactType)
      throws InvalidDescriptorLoaderException {
    return createClassLoaderModel(artifactFile, attributes, artifactType);
  }

  protected ClassLoaderModel createClassLoaderModel(File artifactFile, Map<String, Object> attributes, ArtifactType artifactType)
      throws InvalidDescriptorLoaderException {
    if (isHeavyPackage(artifactFile, attributes)) {
      return createHeavyPackageClassLoaderModel(artifactFile, getClassLoaderModelDescriptor(artifactFile), attributes);
    } else {
      return createLightPackageClassLoaderModel(artifactFile, attributes, artifactType);
    }
  }

  private ClassLoaderModel createHeavyPackageClassLoaderModel(File artifactFile, File classLoaderModelDescriptor,
                                                              Map<String, Object> attributes) {
    return createHeavyPackageClassLoaderModel(artifactFile, classLoaderModelDescriptor, attributes,
                                              of(getDeployableArtifactRepositoryFolder(artifactFile)));
  }

  protected ClassLoaderModel createHeavyPackageClassLoaderModel(File artifactFile, File classLoaderModelDescriptor,
                                                                Map<String, Object> attributes,
                                                                Optional<File> deployableArtifactRepositoryFolder) {
    org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel =
        getPackagerClassLoaderModel(classLoaderModelDescriptor);

    final ArtifactClassLoaderModelBuilder classLoaderModelBuilder =
        newHeavyWeightClassLoaderModelBuilder(artifactFile, (BundleDescriptor) attributes.get(BundleDescriptor.class.getName()),
                                              packagerClassLoaderModel, attributes);
    final Set<String> exportedPackages = new HashSet<>(getAttribute(attributes, EXPORTED_PACKAGES));
    final Set<String> exportedResources = new HashSet<>(getAttribute(attributes, EXPORTED_RESOURCES));

    classLoaderModelBuilder
        .exportingPackages(exportedPackages)
        .exportingResources(exportedResources)
        .exportingPrivilegedPackages(new HashSet<>(getAttribute(attributes, PRIVILEGED_EXPORTED_PACKAGES)),
                                     new HashSet<>(getAttribute(attributes, PRIVILEGED_ARTIFACTS_IDS)));

    Set<BundleDependency> bundleDependencies;
    if (deployableArtifactRepositoryFolder.isPresent()) {
      Set<BundleDependency> patchBundleDependencies =
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
          }).collect(toSet());
    } else {
      bundleDependencies = packagerClassLoaderModel.getDependencies().stream()
          .map(artifact -> createBundleDependencyFromPackagerDependency(uri -> uri).apply(artifact))
          .collect(toSet());
    }

    List<URL> patches = getArtifactPatches(packagerClassLoaderModel);

    // This is already filtering out mule-plugin dependencies,
    // since for this case we explicitly need to consume the exported API from the plugin.
    final List<URL> dependenciesArtifactsUrls = loadUrls(artifactFile, classLoaderModelBuilder, bundleDependencies, patches);

    // TODO MULE-17114 retrieve this data from the json if present, if not then call this
    populateLocalPackages(artifactFile, classLoaderModelBuilder, dependenciesArtifactsUrls, exportedPackages, exportedResources);

    classLoaderModelBuilder.dependingOn(bundleDependencies);

    return classLoaderModelBuilder.build();
  }

  private List<URL> getArtifactPatches(org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel) {
    List<URL> patches = new ArrayList<>();
    ArtifactCoordinates thisArtifactCoordinates = packagerClassLoaderModel.getArtifactCoordinates();
    String artifactId = thisArtifactCoordinates.getGroupId() + ":"
        + thisArtifactCoordinates.getArtifactId() + ":" + thisArtifactCoordinates.getVersion();
    try {
      File muleArtifactPatchesFolder = new File(MuleContainerBootstrapUtils.getMuleHome(), MULE_ARTIFACT_PATCHES_LOCATION);
      if (muleArtifactPatchesFolder.exists()) {
        String[] jarFiles = muleArtifactPatchesFolder.list(new PatternFilenameFilter("*.jar"));
        for (String jarFile : jarFiles) {
          MuleArtifactPatchingModel muleArtifactPatchingModel = MuleArtifactPatchingModel.loadModel(jarFile);
          GenericVersionScheme genericVersionScheme = new GenericVersionScheme();
          Version thisArtifactCoordinatesVersion;
          try {
            thisArtifactCoordinatesVersion = genericVersionScheme.parseVersion(thisArtifactCoordinates.getVersion());
          } catch (Exception e) {
            LOGGER.warn("Error parsing version %s for artifact %s, patches against this artifact will not be applied",
                        thisArtifactCoordinates.getVersion(),
                        thisArtifactCoordinates.getGroupId() + ":" + thisArtifactCoordinates.getArtifactId());
            return emptyList();
          }
          ArtifactCoordinates patchedArtifactCoordinates = muleArtifactPatchingModel.getArtifactCoordinates();
          if (patchedArtifactCoordinates.getGroupId().equals(thisArtifactCoordinates.getGroupId()) &&
              patchedArtifactCoordinates.getArtifactId().equals(thisArtifactCoordinates.getArtifactId()) &&
              patchedArtifactCoordinates.getClassifier().equals(thisArtifactCoordinates.getClassifier())) {
            if (muleArtifactPatchingModel.getAffectedVersions()
                .stream()
                .anyMatch(affectedVersion -> {
                  try {
                    VersionConstraint versionConstraint = genericVersionScheme.parseVersionConstraint(affectedVersion);
                    if (versionConstraint.containsVersion(thisArtifactCoordinatesVersion)) {
                      return true;
                    }
                    return false;
                  } catch (InvalidVersionSpecificationException e) {
                    throw new MuleRuntimeException(createStaticMessage("Could not parse plugin patch affect version: "
                        + affectedVersion), e);
                  }
                })) {
              try {
                URL mulePluginPatchUrl =
                    new File(MuleContainerBootstrapUtils.getMuleHome(),
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
   * Template method to deserialize a {@code classloader-model.json} into the expected
   * {@link org.mule.tools.api.classloader.model.ClassLoaderModel} implementation
   *
   * @param classLoaderModelDescriptor
   * @return a {@link org.mule.tools.api.classloader.model.ClassLoaderModel}
   */
  protected abstract org.mule.tools.api.classloader.model.ClassLoaderModel getPackagerClassLoaderModel(File classLoaderModelDescriptor);

  private Set<BundleDependency> getPatchedBundledDependencies(File artifactFile, File deployableArtifactRepositoryFolder) {
    Set<BundleDependency> patchBundleDependencies = new HashSet<>();
    File classLoaderModelPatchDescriptor = getClassLoaderModelPatchDescriptor(artifactFile);
    if (classLoaderModelPatchDescriptor.exists()) {
      org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModelPatch =
          deserialize(classLoaderModelPatchDescriptor);
      patchBundleDependencies.addAll(packagerClassLoaderModelPatch.getDependencies().stream()
          .map(artifact -> createBundleDependencyFromPackagerDependency(getDeployableArtifactRepositoryUriResolver(deployableArtifactRepositoryFolder))
              .apply(artifact))
          .collect(toSet()));
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

  protected Set<BundleDependency> resolveArtifactDependencies(File artifactFile, Map<String, Object> attributes,
                                                              ArtifactType artifactType) {
    Optional<File> mavenRepository = ofNullable(mavenClient.getMavenConfiguration().getLocalMavenRepositoryLocation());
    if (!mavenRepository.isPresent()) {
      throw new MuleRuntimeException(createStaticMessage(
                                                         format("Missing Maven local repository configuration while trying to resolve class loader model for lightweight artifact: %s",
                                                                artifactFile.getName())));
    }

    boolean includeProvidedDependencies = includeProvidedDependencies(artifactType);
    Optional<MavenReactorResolver> mavenReactorResolver = ofNullable((MavenReactorResolver) attributes
        .get(CLASSLOADER_MODEL_MAVEN_REACTOR_RESOLVER));
    Optional<File> temporaryDirectory = of(createTempDir());
    try {
      List<org.mule.maven.client.api.model.BundleDependency> dependencies =
          mavenClient.resolveArtifactDependencies(artifactFile, includeTestDependencies(attributes),
                                                  includeProvidedDependencies, mavenRepository,
                                                  mavenReactorResolver,
                                                  temporaryDirectory);
      DependencyConverter dependencyConverter = new DependencyConverter();
      return dependencies.stream().map(dependencyConverter::convert).collect(toSet());
    } finally {
      deleteQuietly(temporaryDirectory.get());
    }
  }

  protected ClassLoaderModel createLightPackageClassLoaderModel(File artifactFile,
                                                                Map<String, Object> attributes,
                                                                ArtifactType artifactType) {

    Set<BundleDependency> resolvedDependencies = resolveArtifactDependencies(artifactFile, attributes, artifactType);

    Set<BundleDependency> nonProvidedDependencies =
        resolvedDependencies.stream().filter(dep -> !PROVIDED.equals(dep.getScope())).collect(Collectors.toSet());

    final LightweightClassLoaderModelBuilder classLoaderModelBuilder =
        newLightweightClassLoaderModelBuilder(artifactFile, (BundleDescriptor) attributes.get(BundleDescriptor.class.getName()),
                                              mavenClient, attributes, nonProvidedDependencies);

    final Set<String> exportedPackages = new HashSet<>(getAttribute(attributes, EXPORTED_PACKAGES));
    final Set<String> exportedResources = new HashSet<>(getAttribute(attributes, EXPORTED_RESOURCES));

    classLoaderModelBuilder
        .exportingPackages(exportedPackages)
        .exportingResources(exportedResources)
        .exportingPrivilegedPackages(new HashSet<>(getAttribute(attributes, PRIVILEGED_EXPORTED_PACKAGES)),
                                     new HashSet<>(getAttribute(attributes, PRIVILEGED_ARTIFACTS_IDS)))
        .includeTestDependencies(valueOf(getSimpleAttribute(attributes, INCLUDE_TEST_DEPENDENCIES, "false")));

    // This is already filtering out mule-plugin dependencies,
    // since for this case we explicitly need to consume the exported API from the plugin.
    final List<URL> dependenciesArtifactsUrls =
        loadUrls(artifactFile, classLoaderModelBuilder, nonProvidedDependencies, emptyList());

    // TODO MULE-17114 retrieve this data from the json if present, if not then call this
    populateLocalPackages(artifactFile, classLoaderModelBuilder, dependenciesArtifactsUrls, exportedPackages, exportedResources);

    classLoaderModelBuilder.dependingOn(resolvedDependencies);

    return classLoaderModelBuilder.build();
  }

  protected abstract LightweightClassLoaderModelBuilder newLightweightClassLoaderModelBuilder(File artifactFile,
                                                                                              BundleDescriptor bundleDescriptor,
                                                                                              MavenClient mavenClient,
                                                                                              Map<String, Object> attributes,
                                                                                              Set<BundleDependency> nonProvidedDependencies);

  protected abstract HeavyweightClassLoaderModelBuilder newHeavyWeightClassLoaderModelBuilder(File artifactFile,
                                                                                              BundleDescriptor artifactBundleDescriptor,
                                                                                              org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel,
                                                                                              Map<String, Object> attributes);

  protected void populateLocalPackages(File artifactFile, final ArtifactClassLoaderModelBuilder classLoaderModelBuilder,
                                       List<URL> dependenciesArtifactsUrls,
                                       Set<String> exportedPackages, Set<String> exportedResources) {
    for (URL dependencyArtifactUrl : dependenciesArtifactsUrls) {
      final URI dependencyArtifactUri;
      try {
        dependencyArtifactUri = dependencyArtifactUrl.toURI();
      } catch (URISyntaxException e) {
        throw new MuleRuntimeException(e);
      }

      final JarInfo exploredJar = jarExplorerFactory.get().explore(dependencyArtifactUri);

      final Set<String> localPackages = new HashSet<>(exploredJar.getPackages());
      localPackages.removeAll(exportedPackages);

      final Set<String> localResources = new HashSet<>(exploredJar.getResources());
      localResources.removeAll(exportedResources);

      classLoaderModelBuilder.withLocalPackages(localPackages);
      classLoaderModelBuilder.withLocalResources(localResources);
    }
  }

  protected abstract boolean includeProvidedDependencies(ArtifactType artifactType);

  /**
   * Loads the URLs of the class loader for this artifact.
   * <p>
   * It let's implementations to add artifact specific URLs by letting them override
   * {@link #addArtifactSpecificClassloaderConfiguration(ArtifactClassLoaderModelBuilder)}
   *  @param artifactFile            the artifact file for which the {@link ClassLoaderModel} is being generated.
   * @param classLoaderModelBuilder the builder of the {@link ClassLoaderModel}
   * @param dependencies            the dependencies resolved for this artifact.
   * @param patches
   */
  private List<URL> loadUrls(File artifactFile, ArtifactClassLoaderModelBuilder classLoaderModelBuilder,
                             Set<BundleDependency> dependencies, List<URL> patches) {
    for (URL patchUrl : patches) {
      classLoaderModelBuilder.containing(patchUrl);
    }

    final List<URL> dependenciesArtifactsUrls = new ArrayList<>();

    final URL artifactFileUrl = getUrl(artifactFile, artifactFile);
    classLoaderModelBuilder.containing(artifactFileUrl);
    dependenciesArtifactsUrls.add(artifactFileUrl);

    dependenciesArtifactsUrls.addAll(addArtifactSpecificClassloaderConfiguration(classLoaderModelBuilder));
    dependenciesArtifactsUrls.addAll(addDependenciesToClasspathUrls(artifactFile, classLoaderModelBuilder, dependencies));

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
                                                   ClassLoaderModel.ClassLoaderModelBuilder classLoaderModelBuilder,
                                                   Set<BundleDependency> dependencies) {
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
          classLoaderModelBuilder.containing(dependencyArtifactUrl);
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
    final Object attributeObject = attributes.getOrDefault(attribute, new ArrayList<String>());
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
   * Template method to add artifact specific configuration to the {@link ClassLoaderModel.ClassLoaderModelBuilder}
   *
   * @param classLoaderModelBuilder the builder used to generate {@link ClassLoaderModel} of the artifact.
   */
  protected List<URL> addArtifactSpecificClassloaderConfiguration(ArtifactClassLoaderModelBuilder classLoaderModelBuilder) {
    return emptyList();
  }

}
