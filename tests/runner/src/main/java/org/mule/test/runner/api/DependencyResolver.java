/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.runner.api;

import static java.lang.Boolean.getBoolean;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

import static com.google.common.base.Joiner.on;
import static org.eclipse.aether.ConfigurationProperties.HTTPS_SECURITY_MODE;
import static org.eclipse.aether.ConfigurationProperties.HTTPS_SECURITY_MODE_INSECURE;
import static org.eclipse.aether.util.artifact.ArtifactIdUtils.toId;

import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.maven.client.internal.MuleMavenRepositoryState;
import org.mule.maven.client.internal.MuleMavenRepositoryStateFactory;
import org.mule.maven.client.internal.MuleMavenResolutionContext;
import org.mule.runtime.api.util.Pair;
import org.mule.test.runner.classification.PatternExclusionsDependencyFilter;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.filter.PatternInclusionsDependencyFilter;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.visitor.PathRecordingDependencyVisitor;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides {@link Dependency}s resolutions for Maven {@link Artifact} based on {@link RepositorySystem} and
 * {@link RepositorySystemSession} from Eclipse Aether.
 *
 * @since 4.0
 */
public class DependencyResolver implements AutoCloseable {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final MuleMavenResolutionContext resolutionContext;
  private final MuleMavenRepositoryState repositoryState;
  private final MuleMavenRepositoryStateFactory repositoryStateFactory;
  private boolean closed = false;

  /**
   * Creates an instance of the resolver.
   *
   * @param mavenConfiguration {@link MavenConfiguration} that defines the configuration to be used by Aether.
   * @param workspaceReader    {@link WorkspaceReader} used to resolve {@link Dependency dependencies} within the workspace.
   */
  public DependencyResolver(MavenConfiguration mavenConfiguration, Optional<WorkspaceReader> workspaceReader) {
    requireNonNull(mavenConfiguration, "mavenConfiguration cannot be null");

    this.resolutionContext = new MuleMavenResolutionContext(mavenConfiguration);
    this.repositoryStateFactory = new MuleMavenRepositoryStateFactory();
    Properties userProperties = new Properties();
    userProperties.setProperty(HTTPS_SECURITY_MODE, HTTPS_SECURITY_MODE_INSECURE);
    this.repositoryState =
        repositoryStateFactory.createMavenRepositoryState(resolutionContext.getLocalRepositoryLocation(), workspaceReader,
                                                          resolutionContext.getAuthenticatorSelector(),
                                                          resolutionContext.getProxySelector(),
                                                          resolutionContext.getMirrorSelector(),
                                                          mavenConfiguration.getForcePolicyUpdateNever(),
                                                          mavenConfiguration.getForcePolicyUpdateAlways(),
                                                          mavenConfiguration.getOfflineMode(),
                                                          mavenConfiguration
                                                              .getIgnoreArtifactDescriptorRepositories(),
                                                          Optional.of(userProperties),
                                                          session -> {
                                                          },
                                                          mavenConfiguration.getGlobalChecksumPolicy());

    if (logger.isDebugEnabled()) {
      resolutionContext.getAuthenticatorSelector()
          .ifPresent(selector -> logger.debug("Using authenticator selector: " + ReflectionToStringBuilder.toString(selector)));
    }
  }

  /**
   * Gets information about an artifact like its direct dependencies and potential relocations.
   *
   * @param artifact the {@link Artifact} requested, must not be {@code null}
   * @return {@link ArtifactDescriptorResult} descriptor result, never {@code null}
   * @throws {@link ArtifactDescriptorException} if the artifact descriptor could not be read
   */
  public ArtifactDescriptorResult readArtifactDescriptor(Artifact artifact) throws ArtifactDescriptorException {
    requireNonNull(artifact, "artifact cannot be null");

    final ArtifactDescriptorRequest request =
        new ArtifactDescriptorRequest(artifact, resolveRepositories(), null);
    return repositoryState.getSystem().readArtifactDescriptor(repositoryState.getSession(), request);
  }

  /**
   * Gets information about an artifact like its direct dependencies and potential relocations.
   *
   * @param artifact           the {@link Artifact} requested, must not be {@code null}
   * @param remoteRepositories to be used for resolving the artifact in addition to the ones already defined in context.
   * @return {@link ArtifactDescriptorResult} descriptor result, never {@code null}
   * @throws {@link ArtifactDescriptorException} if the artifact descriptor could not be read
   */
  public ArtifactDescriptorResult readArtifactDescriptor(Artifact artifact, List<RemoteRepository> remoteRepositories)
      throws ArtifactDescriptorException {
    requireNonNull(artifact, "artifact cannot be null");

    final ArtifactDescriptorRequest request =
        new ArtifactDescriptorRequest(artifact, resolveRepositories(remoteRepositories), null);

    return repositoryState.getSystem().readArtifactDescriptor(repositoryState.getSession(), request);
  }

  /**
   * Resolves the path for an artifact.
   *
   * @param artifact the {@link Artifact} requested, must not be {@code null}
   * @return The resolution result, never {@code null}.
   * @throws {@link ArtifactResolutionException} if the artifact could not be resolved.
   */
  public ArtifactResult resolveArtifact(Artifact artifact) throws ArtifactResolutionException {
    requireNonNull(artifact, "artifact cannot be null");

    final ArtifactRequest request = new ArtifactRequest(artifact, resolveRepositories(), null);
    return repositoryState.getSystem().resolveArtifact(repositoryState.getSession(), request);
  }

  /**
   * Resolves the path for an artifact.
   *
   * @param artifact           the {@link Artifact} requested, must not be {@code null}
   * @param remoteRepositories remote repositories to be used in addition to the one in context
   * @return The resolution result, never {@code null}.
   * @throws {@link ArtifactResolutionException} if the artifact could not be resolved.
   */
  public ArtifactResult resolveArtifact(Artifact artifact, List<RemoteRepository> remoteRepositories)
      throws ArtifactResolutionException {
    requireNonNull(artifact, "artifact cannot be null");

    final ArtifactRequest request = new ArtifactRequest(artifact, resolveRepositories(remoteRepositories), null);
    return repositoryState.getSystem().resolveArtifact(repositoryState.getSession(), request);
  }

  /**
   * Resolves direct dependencies for an {@link Artifact}.
   *
   * @param artifact {@link Artifact} to collect its direct dependencies
   * @return a {@link List} of {@link Dependency} for each direct dependency resolved
   * @throws {@link ArtifactDescriptorException} if the artifact descriptor could not be read
   */
  public List<Dependency> getDirectDependencies(Artifact artifact) throws ArtifactDescriptorException {
    requireNonNull(artifact, "artifact cannot be null");

    return readArtifactDescriptor(artifact).getDependencies();
  }

  /**
   * Resolves direct dependencies for an {@link Artifact}.
   *
   * @param artifact           {@link Artifact} to collect its direct dependencies
   * @param remoteRepositories remote repositories to be used in addition to the one in context
   * @return a {@link List} of {@link Dependency} for each direct dependency resolved
   * @throws {@link ArtifactDescriptorException} if the artifact descriptor could not be read
   */
  public List<Dependency> getDirectDependencies(Artifact artifact, List<RemoteRepository> remoteRepositories)
      throws ArtifactDescriptorException {
    requireNonNull(artifact, "artifact cannot be null");

    return readArtifactDescriptor(artifact, remoteRepositories).getDependencies();
  }

  /**
   * Resolves and filters transitive dependencies for the root and direct dependencies for the Mule Runtime container.
   * <p/>
   * If both a root dependency and direct dependencies are given, the direct dependencies will be merged with the direct
   * dependencies from the root dependency's artifact descriptor, giving higher priority to the dependencies from the root.
   *
   * @param root                  {@link Dependency} node from to collect its dependencies, may be {@code null}
   * @param directDependencies    {@link List} of direct {@link Dependency} to collect its transitive dependencies, may be
   *                              {@code null}
   * @param managedDependencies   {@link List} of managed {@link Dependency}s to be used for resolving the depedency graph, may be
   *                              {@code null}
   * @param excludedFilterPattern exclusion patterns in the form of
   *                              {@code [groupId]:[artifactId]:[extension]:[classifier]:[version]}
   * @param remoteRepositories    remote repositories to be used in addition to the one in context.
   * @return the {@link ContainerDependencies} with the {@link URL}s for the container class loader.
   * @throws DependencyCollectionException if the dependency tree could not be built
   * @throws DependencyResolutionException if the dependency tree could not be built or any dependency artifact could not be
   *                                       resolved
   */
  public ContainerDependencies resolveContainerDependencies(Dependency root, List<Dependency> directDependencies,
                                                            List<Dependency> managedDependencies,
                                                            List<String> excludedFilterPattern,
                                                            List<RemoteRepository> remoteRepositories)
      throws DependencyCollectionException, DependencyResolutionException {
    DependencyNode muleApisNode = getMuleApisNode(unmodifiableList(excludedFilterPattern));

    final DependencyFilter dependencyFilter = new PatternExclusionsDependencyFilter(excludedFilterPattern);
    DependencyNode muleLibsNode =
        resolveDependencyNode(root, directDependencies, managedDependencies, dependencyFilter, remoteRepositories);

    return getContainerDependencies(muleApisNode, muleLibsNode);
  }

  private DependencyNode getMuleApisNode(List<String> excludedFilterPattern)
      throws DependencyCollectionException, DependencyResolutionException {
    try {
      final DependencyFilter dependencyFilter = new PatternExclusionsDependencyFilter(excludedFilterPattern);
      final String version = this.getClass().getPackage().getImplementationVersion();
      ArtifactDescriptorResult pom =
          readArtifactDescriptor(new DefaultArtifact("com.mulesoft.mule.distributions", "mule-runtime-apis-split-loader-bom",
                                                     "pom", version));
      return resolveDependencyNode(null, pom.getDependencies(), pom.getManagedDependencies(), dependencyFilter,
                                   pom.getRepositories());
    } catch (ArtifactDescriptorException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Resolves and filters transitive dependencies for the root and direct dependencies.
   * <p/>
   * If both a root dependency and direct dependencies are given, the direct dependencies will be merged with the direct
   * dependencies from the root dependency's artifact descriptor, giving higher priority to the dependencies from the root.
   *
   * @param root                {@link Dependency} node from to collect its dependencies, may be {@code null}
   * @param directDependencies  {@link List} of direct {@link Dependency} to collect its transitive dependencies, may be
   *                            {@code null}
   * @param managedDependencies {@link List} of managed {@link Dependency}s to be used for resolving the depedency graph, may be
   *                            {@code null}
   * @param dependencyFilter    {@link DependencyFilter} to include/exclude dependency nodes during collection and resolve
   *                            operation. May be {@code null} to no filter
   * @param remoteRepositories  {@link RemoteRepository} to be used when resolving dependencies in addition to the ones already
   *                            defined in the context.
   * @return a {@link List} of {@link File}s for each dependency resolved
   * @throws DependencyCollectionException if the dependency tree could not be built
   * @throws DependencyResolutionException if the dependency tree could not be built or any dependency artifact could not be
   *                                       resolved
   */
  public List<File> resolveDependencies(Dependency root, List<Dependency> directDependencies,
                                        List<Dependency> managedDependencies,
                                        DependencyFilter dependencyFilter,
                                        List<RemoteRepository> remoteRepositories)
      throws DependencyCollectionException, DependencyResolutionException {
    DependencyNode node =
        resolveDependencyNode(root, directDependencies, managedDependencies, dependencyFilter, remoteRepositories);

    return getFiles(node);
  }

  private DependencyNode resolveDependencyNode(Dependency root, List<Dependency> directDependencies,
                                               List<Dependency> managedDependencies, DependencyFilter dependencyFilter,
                                               List<RemoteRepository> remoteRepositories)
      throws DependencyCollectionException, DependencyResolutionException {
    CollectRequest collectRequest = new CollectRequest();
    collectRequest.setRoot(root);
    collectRequest.setDependencies(directDependencies);
    collectRequest.setManagedDependencies(managedDependencies);
    collectRequest.setRepositories(resolveRepositories(remoteRepositories));

    DependencyNode node;
    try {
      logger.debug("Collecting dependencies for '{}'", printCollectRequest(collectRequest));
      node = repositoryState.getSystem().collectDependencies(repositoryState.getSession(), collectRequest).getRoot();
      logDependencyGraph(node, collectRequest);
      DependencyRequest dependencyRequest = new DependencyRequest();
      dependencyRequest.setRoot(node);
      dependencyRequest.setCollectRequest(collectRequest);
      dependencyRequest.setFilter((node1, parents) -> !node1.getData().containsKey(ConflictResolver.CONFIG_PROP_VERBOSE)
          && (dependencyFilter == null || dependencyFilter.accept(node1, parents)));

      node = repositoryState.getSystem().resolveDependencies(repositoryState.getSession(), dependencyRequest).getRoot();
    } catch (DependencyResolutionException e) {
      logger.warn("Dependencies couldn't be resolved for request '{}', {}", collectRequest, e.getMessage());
      node = e.getResult().getRoot();
      logUnresolvedArtifacts(node, e);
      throw e;
    }
    return node;
  }

  private String printCollectRequest(CollectRequest collectRequest) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("[");
    Iterator<RemoteRepository> iterator = collectRequest.getRepositories().iterator();
    while (iterator.hasNext()) {
      RemoteRepository remoteRepository = iterator.next();
      stringBuilder.append(remoteRepository);
      stringBuilder.append("->authentication[");
      if (remoteRepository.getAuthentication() != null) {
        stringBuilder.append(remoteRepository.getAuthentication());
      }
      stringBuilder.append("]");
      if (iterator.hasNext()) {
        stringBuilder.append(", ");
      }
    }
    stringBuilder.append("]");
    return collectRequest.getRoot() + " -> " + collectRequest.getDependencies() + " < " + stringBuilder.toString();
  }

  private List<RemoteRepository> resolveRepositories() {
    return resolveRepositories(emptyList());
  }

  private List<RemoteRepository> resolveRepositories(List<RemoteRepository> remoteRepositories) {
    return repositoryState.getSystem().newResolutionRepositories(repositoryState.getSession(),
                                                                 Stream
                                                                     .of(remoteRepositories,
                                                                         resolutionContext.getRemoteRepositories())
                                                                     .flatMap(Collection::stream).collect(toList()));
  }

  private void logDependencyGraph(DependencyNode node, Object request) {
    if (logger.isTraceEnabled()) {
      PathRecordingDependencyVisitor visitor = new PathRecordingDependencyVisitor(null, false);
      node.accept(visitor);

      logger.trace("******* Dependency Graph calculated for {} with request: '{}' *******", request.getClass().getSimpleName(),
                   request);
      visitor.getPaths().stream().forEach(
                                          pathList -> logger.trace(on(" -> ")
                                              .join(pathList.stream().filter(path -> path != null).collect(toList()))));
      logger.trace("******* End of dependency Graph *******");
    }
  }

  /**
   * Traverse the {@link DependencyNode} to get the files for each artifact.
   *
   * @param node {@link DependencyNode} that represents the dependency graph
   * @return {@link List} of {@link File}s for each artifact resolved
   */
  private List<File> getFiles(DependencyNode node) {
    PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
    node.accept(nlg);

    return nlg.getFiles().stream()
        .map(File::getAbsoluteFile)
        .distinct()
        .collect(toList());
  }

  /**
   * Traverse the {@link DependencyNode} to get the files for each artifact.
   *
   * @param muleApisNode
   * @param muleLibsNode {@link DependencyNode} that represents the dependency graph
   * @return a {@link Pair} with {@link List}s of {@link URL}s for the container class loader. First are mule jars urls, second
   *         are jar urls for third parties.
   */
  private ContainerDependencies getContainerDependencies(DependencyNode muleApisNode, DependencyNode muleLibsNode) {
    LinkedHashSet<URL> muleApisDependencyUrls = new LinkedHashSet<>();
    LinkedHashSet<URL> muleApisOptDependencyUrls = new LinkedHashSet<>();
    LinkedHashSet<URL> muleDependencyUrls = new LinkedHashSet<>();
    LinkedHashSet<URL> optDependencyUrls = new LinkedHashSet<>();

    splitMuleAndOpt(muleApisNode, muleApisDependencyUrls, muleApisOptDependencyUrls);
    splitMuleAndOpt(muleLibsNode, muleDependencyUrls, optDependencyUrls);

    // sanitize dependencies
    optDependencyUrls.removeAll(muleApisOptDependencyUrls);
    muleDependencyUrls.removeAll(muleApisDependencyUrls);

    // move log4j dependencies to the upper third-party layer (they're not directly present in the BOM because it's not something
    // for it to provide, the client must do it)
    List<URL> log4jUrls =
        optDependencyUrls.stream().filter(url -> url.toString().contains("log4j") || url.toString().contains("lmax"))
            .collect(toList());
    muleApisOptDependencyUrls.addAll(log4jUrls);
    log4jUrls.forEach(optDependencyUrls::remove);

    return new ContainerDependencies(new ArrayList<>(muleApisOptDependencyUrls), new ArrayList<>(muleApisDependencyUrls),
                                     new ArrayList<>(optDependencyUrls), new ArrayList<>(muleDependencyUrls));
  }

  private void splitMuleAndOpt(DependencyNode node, Set<URL> muleDependencyUrls, Set<URL> optDependencyUrls) {
    PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
    node.accept(nlg);

    nlg.getNodes()
        .stream()
        .forEach(depNode -> {
          final Artifact artifact = depNode.getArtifact();
          if (artifact.getFile() == null) {
            return;
          }

          if (artifact.getGroupId().equals("com.mulesoft.connectivity")) {
            logger.error("DependencyResolver dependency found: {} : {} : {}", artifact.getArtifactId(), artifact.getFile(),
                         artifact.getClassifier());
          } else {
            logger.error("com.mulesoft.connectivity group not found");
          }

          final File absoluteFile = artifact.getFile().getAbsoluteFile();

          if (isMuleContainerGroupId(artifact.getGroupId())) {
            muleDependencyUrls.add(toUrl(absoluteFile));
          } else {
            optDependencyUrls.add(toUrl(absoluteFile));
          }
        });
  }

  /**
   * Converts the {@link File} to {@link URL}
   *
   * @param file {@link File} to get its {@link URL}
   * @return {@link URL} for the file
   */
  private URL toUrl(File file) {
    try {
      return file.toURI().toURL();
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Couldn't get URL", e);
    }
  }

  /**
   * Groups of dependencies that form the Runtime's container class loader.
   */
  public static class ContainerDependencies {

    private final List<URL> muleApisDependencyUrls;
    private final List<URL> muleApisOptDependencyUrls;
    private final List<URL> muleDependencyUrls;
    private final List<URL> optDependencyUrls;

    ContainerDependencies(List<URL> muleApisDependencyUrls, List<URL> muleApisOptDependencyUrls, List<URL> optDependencyUrls,
                          List<URL> muleDependencyUrls) {
      this.muleApisDependencyUrls = unmodifiableList(muleApisDependencyUrls);
      this.muleApisOptDependencyUrls = unmodifiableList(muleApisOptDependencyUrls);
      this.muleDependencyUrls = unmodifiableList(muleDependencyUrls);
      this.optDependencyUrls = unmodifiableList(optDependencyUrls);
    }

    public List<URL> getMuleApisDependencyUrls() {
      return muleApisDependencyUrls;
    }

    public List<URL> getMuleApisOptDependencyUrls() {
      return muleApisOptDependencyUrls;
    }

    public List<URL> getOptDependencyUrls() {
      return optDependencyUrls;
    }

    public List<URL> getMuleDependencyUrls() {
      return muleDependencyUrls;
    }
  }

  // Implementation note: this must be kept consistent with the equivalent logic in embedded-api and the distro assemblies
  private boolean isMuleContainerGroupId(final String groupId) {
    return groupId.equals("org.mule.runtime")
        || groupId.equals("org.mule.runtime.boot")
        || groupId.equals("org.mule.sdk")
        || groupId.equals("org.mule.weave")
        || groupId.equals("org.mule.commons")
        || groupId.equals("com.mulesoft.connectivity")
        || groupId.equals("com.mulesoft.mule.runtime")
        || groupId.equals("com.mulesoft.mule.runtime.boot")
        || groupId.equals("com.mulesoft.mule.runtime.modules")
        || groupId.equals("com.mulesoft.anypoint");
  }

  /**
   * Logs the paths for each dependency not found
   *
   * @param node root {@link DependencyNode}, can be a "null" root (imaginary root)
   * @param e    {@link DependencyResolutionException} the error to collect paths.
   */
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
        logger.warn("Dependency path to not resolved artifacts -> {}", unresolvedArtifactPath.toString());
      }
    });
  }

  @Override
  public void close() throws Exception {
    if (!closed) {
      repositoryStateFactory.close();
      closed = true;
    }
  }

}
