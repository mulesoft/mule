/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.artifact;

import static java.lang.Boolean.getBoolean;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.apache.maven.repository.internal.MavenRepositorySystemUtils.newSession;
import static org.eclipse.aether.repository.RepositoryPolicy.CHECKSUM_POLICY_IGNORE;
import static org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_NEVER;
import static org.eclipse.aether.resolution.ArtifactDescriptorPolicy.STRICT;
import static org.eclipse.aether.util.artifact.ArtifactIdUtils.toId;
import static org.eclipse.aether.util.artifact.JavaScopes.PROVIDED;
import static org.eclipse.aether.util.artifact.JavaScopes.TEST;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.container.api.MuleFoldersUtil.getMuleHomeFolder;
import static org.mule.runtime.core.util.JarUtils.loadFileContentFrom;
import static org.mule.runtime.deployment.model.api.application.ApplicationDescriptor.REPOSITORY_FOLDER;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.MAVEN;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.getMavenLocalRepository;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.getPomModelFromJar;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.getPomUrlFromJar;
import static org.mule.runtime.module.reboot.MuleContainerBootstrapUtils.isStandalone;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants;
import org.mule.runtime.deployment.model.internal.plugin.BundlePluginDependenciesResolver;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel.ClassLoaderModelBuilder;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModelLoader;
import org.mule.runtime.module.artifact.descriptor.InvalidDescriptorLoaderException;

import com.google.common.collect.ImmutableList;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.maven.model.Model;
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.internal.impl.DefaultRepositorySystem;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.filter.PatternInclusionsDependencyFilter;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;
import org.eclipse.aether.util.graph.visitor.PathRecordingDependencyVisitor;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible of returning the {@link BundleDescriptor} of a given plugin's location and also creating a
 * {@link ClassLoaderModel}
 * <p>
 * TODO(fernandezlautaro): MULE-11094 this class is the default implementation for discovering dependencies and URLs, which
 * happens to be Maven based. There could be other ways to look for dependencies and URLs (probably for testing purposes where the
 * plugins are done by hand and without maven) which will imply implementing the jira pointed out in this comment.
 *
 * @since 4.0
 */
// TODO MULE-11878 - consolidate with other aether usages in mule.
public abstract class MavenClassLoaderModelLoader implements ClassLoaderModelLoader {

  private static final String MULE_REMOTE_REPOSITORIES_PROPERTY = "mule.repository.repositories";
  private static final String DEFAULT_REPOSITORY_TYPE = "default";
  private static final String REPOSITORY_DIRECTORY_NAME = "repository";
  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

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
   * @see BundlePluginDependenciesResolver#getArtifactsWithDependencies(Set, Set)
   */
  @Override
  public final ClassLoaderModel load(File artifactFile, Map<String, Object> attributes)
      throws InvalidDescriptorLoaderException {
    Model model = loadPomModel(artifactFile);
    return createClassLoaderModel(artifactFile, attributes, model);
  }

  private ClassLoaderModel createClassLoaderModel(File artifactFile, Map<String, Object> attributes, Model model)
      throws InvalidDescriptorLoaderException {
    final ClassLoaderModelBuilder classLoaderModelBuilder = new ClassLoaderModelBuilder();
    classLoaderModelBuilder
        .exportingPackages(new HashSet<>(getAttribute(attributes, EXPORTED_PACKAGES)))
        .exportingResources(new HashSet<>(getAttribute(attributes, EXPORTED_RESOURCES)));
    final DependencyResult dependencyResult = assemblyDependenciesFromPom(artifactFile, model);
    final PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
    // This adds a ton of things that not always make sense
    dependencyResult.getRoot().accept(nlg);
    Set<BundleDependency> dependencies = loadDependencies(dependencyResult, nlg);
    classLoaderModelBuilder.dependingOn(dependencies);
    loadUrls(artifactFile, classLoaderModelBuilder, dependencyResult, nlg, dependencies);
    return classLoaderModelBuilder.build();
  }

  /**
   * Loads the {@link ClassLoaderModel} from an artifact with the provided maven pom model.
   * 
   * @param artifactFile the artifact folder
   * @param mavenModel the pom model
   * @return a {@link ClassLoaderModel} loaded with all its dependencies and URLs.
   * @throws InvalidDescriptorLoaderException
   */
  public final ClassLoaderModel load(File artifactFile, Model mavenModel) throws InvalidDescriptorLoaderException {
    return createClassLoaderModel(artifactFile, emptyMap(), mavenModel);
  }

  /**
   * Template method to get the pom model from the artifact file
   *
   * @param artifactFile the artifact file
   * @return the pom model
   */
  protected Model loadPomModel(File artifactFile) {
    return getPomModelFromJar(artifactFile);
  }

  protected abstract Set<BundleDependency> loadDependencies(DependencyResult dependencyResult, PreorderNodeListGenerator nlg);

  protected abstract void loadUrls(File pluginFolder, ClassLoaderModelBuilder classLoaderModelBuilder,
                                   DependencyResult dependencyResult, PreorderNodeListGenerator nlg,
                                   Set<BundleDependency> dependencies);

  private DependencyResult assemblyDependenciesFromPom(File artifactFolder, Model model)
      throws InvalidDescriptorLoaderException {


    boolean isTestDependenciesEnabled = enabledTestDependencies();

    Artifact defaultArtifact = new DefaultArtifact(model.getGroupId(), model.getArtifactId(),
                                                   null,
                                                   "pom",
                                                   model.getVersion() != null ? model.getVersion()
                                                       : model.getParent().getVersion());

    RepositoryState repositoryState = new RepositoryState(artifactFolder, defaultArtifact);
    final CollectRequest currentPluginRequest = new CollectRequest();
    try {
      final ArtifactDescriptorResult artifactDescriptorResult =
          repositoryState.getSystem().readArtifactDescriptor(repositoryState.getSession(),
                                                             new ArtifactDescriptorRequest(defaultArtifact, null, null)
                                                                 .setRepositories(collectRemoteRepositories()));
      List<Dependency> dependencies = artifactDescriptorResult.getDependencies();
      List<Dependency> dependenciesWithExclusions = new ArrayList<>();
      dependencies.stream()
          .filter(dependency -> {
            return isTestDependenciesEnabled || !dependency.getScope().equalsIgnoreCase("test");
          })
          .forEach(dependency -> {
            if (MULE_PLUGIN_CLASSIFIER.equals(dependency.getArtifact().getClassifier())) {
              dependenciesWithExclusions.add(dependency.setExclusions(asList(new Exclusion("*", "*", "*", "*"))));
            } else {
              dependenciesWithExclusions.add(dependency);
            }
          });
      currentPluginRequest.setDependencies(dependenciesWithExclusions);
      currentPluginRequest.setManagedDependencies(artifactDescriptorResult.getManagedDependencies());
      currentPluginRequest
          .setRepositories(ImmutableList.<RemoteRepository>builder()
              .add(new RemoteRepository.Builder("localhost-repository", "default",
                                                getMavenLocalRepository().toURI().toString())
                                                    .build())
              .addAll(collectRemoteRepositories()).build());

      final CollectResult collectResult =
          repositoryState.getSystem().collectDependencies(repositoryState.getSession(), currentPluginRequest);

      final DependencyRequest currentPluginDependenciesRequest = new DependencyRequest();
      currentPluginDependenciesRequest.setFilter(new ScopeDependencyFilter(isTestDependenciesEnabled ? new String[] {PROVIDED}
          : new String[] {PROVIDED, TEST}));
      currentPluginDependenciesRequest.setRoot(collectResult.getRoot());
      currentPluginDependenciesRequest.setCollectRequest(currentPluginRequest);
      final DependencyResult dependencyResult =
          repositoryState.getSystem().resolveDependencies(repositoryState.getSession(), currentPluginDependenciesRequest);
      return dependencyResult;
    } catch (DependencyResolutionException e) {
      DependencyNode node = e.getResult().getRoot();
      logUnresolvedArtifacts(node, e);
      throw new InvalidDescriptorLoaderException(format("There was an issue solving the dependencies for the plugin [%s]",
                                                        artifactFolder.getAbsolutePath()),
                                                 e);
    } catch (DependencyCollectionException e) {
      throw new InvalidDescriptorLoaderException(format("There was an issue resolving the dependency tree for the plugin [%s]",
                                                        artifactFolder.getAbsolutePath()),
                                                 e);
    } catch (ArtifactDescriptorException e) {
      throw new ArtifactDescriptorCreateException(format("There was an issue resolving the artifact descriptor for the plugin [%s]",
                                                         artifactFolder.getAbsolutePath()),
                                                  e);
    }
  }


  private List<RemoteRepository> collectRemoteRepositories() {
    String[] remoteRepositoriesArray = System.getProperty(MULE_REMOTE_REPOSITORIES_PROPERTY, "").split(",");
    List<RemoteRepository> remoteRepositories = new ArrayList<>();
    for (String remoteRepository : remoteRepositoriesArray) {
      if (!remoteRepository.trim().equals("")) {
        remoteRepositories
            .add(new RemoteRepository.Builder(remoteRepository, DEFAULT_REPOSITORY_TYPE, remoteRepository.trim()).build());
      }
    }
    return remoteRepositories;
  }

  /**
   * Template method to enable/disable test dependencies as part of the artifact classpath.
   *
   * @return true if test dependencies must be part of the artifact classpath, false otherwise.
   */
  protected boolean enabledTestDependencies() {
    return false;
  }

  private List<String> getAttribute(Map<String, Object> attributes, String attribute) {
    final Object attributeObject = attributes.getOrDefault(attribute, new ArrayList<String>());
    checkArgument(attributeObject instanceof List, format("The '%s' attribute must be of '%s', found '%s'", attribute,
                                                          List.class.getName(), attributeObject.getClass().getName()));
    return (List<String>) attributeObject;
  }

  /**
   * Determines the local repository location.
   * <p>
   * If the artifact is being loaded from an application repository, then such repository must be used as the local maven
   * repository.
   * <p>
   * If the artifact doesn't belong to an application repository then the container repository must be used instead.
   * <p>
   * When the 'mule.mode.embedded' is enabled, then the user local repository must be used.
   *
   * @param artifactFile the artifact file which dependencies are being resolved.
   * @return the location of the local repository.
   */
  private File determineLocalRepositoryPath(File artifactFile) {
    File mavenLocalRepositoryDirectory = getMavenLocalRepository();
    File possibleApplicationRepository = new File(artifactFile, REPOSITORY_FOLDER);
    if (artifactFile.isDirectory() && possibleApplicationRepository.exists()) {
      return possibleApplicationRepository;
    }
    if (artifactFile.getAbsolutePath().contains(REPOSITORY_DIRECTORY_NAME)
        && !artifactFile.getAbsolutePath().contains(mavenLocalRepositoryDirectory.getAbsolutePath())) {
      while (!artifactFile.getParentFile().getName().equals(REPOSITORY_DIRECTORY_NAME)) {
        artifactFile = artifactFile.getParentFile();
      }
      return artifactFile.getParentFile();
    } else {
      if (isStandalone() && !getBoolean("mule.mode.embedded")) {
        File localRepositoryDirectory = new File(getMuleHomeFolder(), "repository");
        if (!localRepositoryDirectory.exists()) {
          if (!localRepositoryDirectory.mkdirs()) {
            // check again since it may have been created already.
            if (!localRepositoryDirectory.exists()) {
              throw new MuleRuntimeException(I18nMessageFactory
                  .createStaticMessage("Failure creating repository folder in MULE_HOME folder "
                      + localRepositoryDirectory.getAbsolutePath()));
            }
          }
        }
        return localRepositoryDirectory;
      }
      return mavenLocalRepositoryDirectory;
    }
  }

  private static DefaultRepositorySystemSession newDefaultRepositorySystemSession() {
    final DefaultRepositorySystemSession session = newSession();
    session.setUpdatePolicy(UPDATE_POLICY_NEVER);
    session.setChecksumPolicy(CHECKSUM_POLICY_IGNORE);
    return session;
  }

  private void logUnresolvedArtifacts(DependencyNode node, DependencyResolutionException e) {
    List<ArtifactResult> artifactResults = e.getResult().getArtifactResults().stream()
        .filter(artifactResult -> !artifactResult.getExceptions().isEmpty()).collect(toList());

    final List<String> patternInclusion =
        artifactResults.stream().map(artifactResult -> toId(artifactResult.getRequest().getArtifact())).collect(toList());

    PathRecordingDependencyVisitor visitor =
        new PathRecordingDependencyVisitor(new PatternInclusionsDependencyFilter(patternInclusion), node.getArtifact() != null);
    node.accept(visitor);

    visitor.getPaths().stream().forEach(path -> {
      List<DependencyNode> unresolvedArtifactPath =
          path.stream().filter(dependencyNode -> dependencyNode.getArtifact() != null).collect(toList());
      if (!unresolvedArtifactPath.isEmpty()) {
        logger.warn("Dependency path to not resolved artifacts -> " + unresolvedArtifactPath.toString());
      }
    });
  }

  /**
   * Inner class that holds the state for aether repository state for the resolution of dependencies of a particular artifact.
   */
  private class RepositoryState {

    private DefaultRepositorySystemSession session;
    private RepositorySystem system;

    public RepositoryState(File artifactFile, Artifact pluginArtifact) {
      session = newDefaultRepositorySystemSession();
      RepositorySystem repositorySystem = createRepositorySystem();

      File localRepositoryDir = determineLocalRepositoryPath(artifactFile);
      session.setLocalRepositoryManager(repositorySystem
          .newLocalRepositoryManager(session, new LocalRepository(localRepositoryDir)));
      session.setOffline(false);
      session.setArtifactDescriptorPolicy((session, request) -> STRICT);
      session.setIgnoreArtifactDescriptorRepositories(true);
      session.setWorkspaceReader(new PomWorkspaceReader(localRepositoryDir, artifactFile, pluginArtifact));
      system = repositorySystem;
    }

    private RepositorySystem createRepositorySystem() {
      DefaultServiceLocator locator = new DefaultServiceLocator();
      locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
      locator.addService(TransporterFactory.class, FileTransporterFactory.class);
      locator.addService(RepositorySystem.class, DefaultRepositorySystem.class);
      locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
      locator.addService(VersionResolver.class, DefaultVersionResolver.class);
      locator.addService(VersionRangeResolver.class, DefaultVersionRangeResolver.class);
      locator.addService(ArtifactDescriptorReader.class, DefaultArtifactDescriptorReader.class);
      locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {

        @Override
        public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
          logger.warn(exception.getMessage());
          if (logger.isDebugEnabled()) {
            logger.debug(exception.getMessage(), exception);
          }
        }
      });
      return locator.getService(RepositorySystem.class);
    }

    public DefaultRepositorySystemSession getSession() {
      return session;
    }

    public RepositorySystem getSystem() {
      return system;
    }
  }

  /**
   * Custom implementation of a {@link WorkspaceReader} meant to be tightly used with the plugin mechanism, where the POM file is
   * inside the {@link ArtifactPluginDescriptor#MULE_ARTIFACT_FOLDER}. For any other {@link Artifact} it will return values that
   * will force the dependency mechanism to look for in a different {@link WorkspaceReader}
   *
   * @since 4.0
   */
  private class PomWorkspaceReader implements WorkspaceReader {

    private final File artifactFile;
    private final Artifact pluginArtifact;
    final WorkspaceRepository workspaceRepository;
    private final File localRepositoryDirectory;

    /**
     * @param localRepositoryDirectory directory of the maven repo to load this artifact dependencies from
     * @param artifactContent artifacts File with it's content used to look for the POM file
     * @param pluginArtifact plugin's artifact to compare, so that resolves the file in {@link #findArtifact(Artifact)} when it
     *        matches with the {@link #pluginArtifact}
     */
    PomWorkspaceReader(File localRepositoryDirectory, File artifactContent, Artifact pluginArtifact) {
      this.localRepositoryDirectory = localRepositoryDirectory;
      this.artifactFile = artifactContent;
      this.pluginArtifact = pluginArtifact;
      this.workspaceRepository = new WorkspaceRepository(format("worskpace-repository-%s", artifactContent.getName()));
    }

    @Override
    public WorkspaceRepository getRepository() {
      return workspaceRepository;
    }

    @Override
    public File findArtifact(Artifact artifact) {
      if (checkArtifact(artifact)) {
        if (!artifactFile.isDirectory()) {
          try {
            URL pomUrl = getPomUrlFromJar(artifactFile);
            Optional<byte[]> pomContentOptional = loadFileContentFrom(pomUrl);
            byte[] pomBytes = pomContentOptional.orElseThrow(() -> new MuleRuntimeException(I18nMessageFactory
                .createStaticMessage(format("No pom file found in %s", artifactFile))));
            File pomLocation = new File(localRepositoryDirectory, format(".mule/plugins/%s/%s/%s/%s-%s",
                                                                         artifact.getGroupId(), artifact.getArtifactId(),
                                                                         artifact.getVersion(),
                                                                         artifact.getArtifactId(), artifact.getVersion()));
            copyInputStreamToFile(new ByteArrayInputStream(pomBytes), pomLocation);
            return pomLocation;
          } catch (IOException e) {
            throw new MuleRuntimeException(e);
          }
        } else {
          String pathToPom =
              Paths.get("META-INF", "maven", artifact.getGroupId(), artifact.getArtifactId(), "pom.xml").toString();
          return new File(artifactFile, pathToPom);
        }
      }
      return null;
    }

    @Override
    public List<String> findVersions(Artifact artifact) {
      if (checkArtifact(artifact)) {
        return singletonList(artifact.getVersion());
      }
      return emptyList();
    }

    private boolean checkArtifact(Artifact artifact) {
      return pluginArtifact.getGroupId().equals(artifact.getGroupId())
          && pluginArtifact.getArtifactId().equals(artifact.getArtifactId())
          && pluginArtifact.getVersion().equals(artifact.getVersion())
          && pluginArtifact.getExtension().equals(artifact.getExtension());
    }
  }
}
