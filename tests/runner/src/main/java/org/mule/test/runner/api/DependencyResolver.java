/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static com.google.common.base.Joiner.on;
import static java.util.stream.Collectors.toList;
import static org.eclipse.aether.util.artifact.ArtifactIdUtils.toId;
import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.maven.client.internal.AetherRepositoryState;
import org.mule.maven.client.internal.AetherResolutionContext;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
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
public class DependencyResolver {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  private AetherResolutionContext resolutionContext;
  private AetherRepositoryState repositoryState;

  /**
   * Creates an instance of the resolver.
   *
   * @param mavenConfiguration {@link MavenConfiguration} that defines the configuration to be used by Aether.
   * @param workspaceReader {@link WorkspaceReader} used to resolve {@link Dependency dependencies} within  the workspace.
   */
  public DependencyResolver(MavenConfiguration mavenConfiguration, Optional<WorkspaceReader> workspaceReader) {
    checkNotNull(mavenConfiguration, "mavenConfiguration cannot be null");

    this.resolutionContext = new AetherResolutionContext(mavenConfiguration);
    this.repositoryState =
        new AetherRepositoryState(this.resolutionContext.getLocalRepositoryLocation(), workspaceReader, false,
                                  true, resolutionContext.getAuthenticatorSelector(), true);

    logger.info(ReflectionToStringBuilder.toString(resolutionContext.getAuthenticatorSelector()));
  }

  /**
   * Gets information about an artifact like its direct dependencies and potential relocations.
   *
   * @param artifact the {@link Artifact} requested, must not be {@code null}
   * @return {@link ArtifactDescriptorResult} descriptor result, never {@code null}
   * @throws {@link ArtifactDescriptorException} if the artifact descriptor could not be read
   */
  public ArtifactDescriptorResult readArtifactDescriptor(Artifact artifact) throws ArtifactDescriptorException {
    checkNotNull(artifact, "artifact cannot be null");

    final ArtifactDescriptorRequest request =
        new ArtifactDescriptorRequest(artifact, resolutionContext.getRemoteRepositories(), null);
    return repositoryState.getSystem().readArtifactDescriptor(repositoryState.getSession(), request);
  }

  /**
   * Gets information about an artifact like its direct dependencies and potential relocations.
   *
   * @param artifact the {@link Artifact} requested, must not be {@code null}
   * @param remoteRepositories to be used for resolving the artifact in addition to the ones already defined in context.
   * @return {@link ArtifactDescriptorResult} descriptor result, never {@code null}
   * @throws {@link ArtifactDescriptorException} if the artifact descriptor could not be read
   */
  public ArtifactDescriptorResult readArtifactDescriptor(Artifact artifact, List<RemoteRepository> remoteRepositories)
      throws ArtifactDescriptorException {
    checkNotNull(artifact, "artifact cannot be null");

    final ArtifactDescriptorRequest request =
        new ArtifactDescriptorRequest(artifact, resolutionContext.getRemoteRepositories(), null);
    // Has to set authentication to these remote repositories as they may come from a pom descriptor
    remoteRepositories.forEach(remoteRepository -> {
      RemoteRepository authenticatedRemoteRepository = setAuthentication(remoteRepository);
      if (!request.getRepositories().contains(authenticatedRemoteRepository)) {
        request.addRepository(authenticatedRemoteRepository);
      }
    });

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
    checkNotNull(artifact, "artifact cannot be null");

    final ArtifactRequest request = new ArtifactRequest(artifact, resolutionContext.getRemoteRepositories(), null);
    return repositoryState.getSystem().resolveArtifact(repositoryState.getSession(), request);
  }

  /**
   * Resolves the path for an artifact.
   *
   * @param artifact the {@link Artifact} requested, must not be {@code null}
   * @param remoteRepositories remote repositories to be used in addition to the one in context
   * @return The resolution result, never {@code null}.
   * @throws {@link ArtifactResolutionException} if the artifact could not be resolved.
   */
  public ArtifactResult resolveArtifact(Artifact artifact, List<RemoteRepository> remoteRepositories)
      throws ArtifactResolutionException {
    checkNotNull(artifact, "artifact cannot be null");

    final ArtifactRequest request = new ArtifactRequest(artifact, resolutionContext.getRemoteRepositories(), null);
    // Has to set authentication to these remote repositories as they may come from a pom descriptor
    remoteRepositories.forEach(remoteRepository -> {
      RemoteRepository authenticatedRemoteRepository = setAuthentication(remoteRepository);
      if (!request.getRepositories().contains(authenticatedRemoteRepository)) {
        request.addRepository(authenticatedRemoteRepository);
      }
    });
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
    checkNotNull(artifact, "artifact cannot be null");

    return readArtifactDescriptor(artifact).getDependencies();
  }

  /**
   * Resolves direct dependencies for an {@link Artifact}.
   *
   * @param artifact {@link Artifact} to collect its direct dependencies
   * @param remoteRepositories remote repositories to be used in addition to the one in context
   * @return a {@link List} of {@link Dependency} for each direct dependency resolved
   * @throws {@link ArtifactDescriptorException} if the artifact descriptor could not be read
   */
  public List<Dependency> getDirectDependencies(Artifact artifact, List<RemoteRepository> remoteRepositories)
      throws ArtifactDescriptorException {
    checkNotNull(artifact, "artifact cannot be null");

    return readArtifactDescriptor(artifact, remoteRepositories).getDependencies();
  }

  /**
   * Resolves and filters transitive dependencies for the root and direct dependencies.
   * <p/>
   * If both a root dependency and direct dependencies are given, the direct dependencies will be merged with the direct
   * dependencies from the root dependency's artifact descriptor, giving higher priority to the dependencies from the root.
   *
   * @param root {@link Dependency} node from to collect its dependencies, may be {@code null}
   * @param directDependencies {@link List} of direct {@link Dependency} to collect its transitive dependencies, may be
   *        {@code null}
   * @param managedDependencies {@link List} of managed {@link Dependency}s to be used for resolving the depedency graph, may be
   *        {@code null}
   * @param dependencyFilter {@link DependencyFilter} to include/exclude dependency nodes during collection and resolve operation.
   *        May be {@code null} to no filter
   * @param remoteRepositories {@link RemoteRepository} to be used when resolving dependencies in addition to the ones already defined in the context.
   * @return a {@link List} of {@link File}s for each dependency resolved
   * @throws {@link DependencyCollectionException} if the dependency tree could not be built
   * @thwows {@link DependencyResolutionException} if the dependency tree could not be built or any dependency artifact could not
   *         be resolved
   */
  public List<File> resolveDependencies(Dependency root, List<Dependency> directDependencies,
                                        List<Dependency> managedDependencies,
                                        DependencyFilter dependencyFilter, List<RemoteRepository> remoteRepositories)
      throws DependencyCollectionException, DependencyResolutionException {
    CollectRequest collectRequest = new CollectRequest();
    collectRequest.setRoot(root);
    collectRequest.setDependencies(directDependencies);
    collectRequest.setManagedDependencies(managedDependencies);
    collectRequest.setRepositories(resolutionContext.getRemoteRepositories());
    // Has to set authentication to these remote repositories as they may come from a pom descriptor
    remoteRepositories.forEach(remoteRepository -> {
      RemoteRepository authenticatedRemoteRepository = setAuthentication(remoteRepository);
      if (!collectRequest.getRepositories().contains(authenticatedRemoteRepository)) {
        collectRequest.addRepository(authenticatedRemoteRepository);
      }
    });

    DependencyNode node;
    try {
      logger.info("Collecting dependencies for '{}'", printCollectRequest(collectRequest));
      node = repositoryState.getSystem().collectDependencies(repositoryState.getSession(), collectRequest).getRoot();
      logDependencyGraph(node, collectRequest);
      DependencyRequest dependencyRequest = new DependencyRequest();
      dependencyRequest.setRoot(node);
      dependencyRequest.setCollectRequest(collectRequest);
      if (dependencyFilter != null) {
        dependencyRequest.setFilter(dependencyFilter);
      }

      node = repositoryState.getSystem().resolveDependencies(repositoryState.getSession(), dependencyRequest).getRoot();
    } catch (DependencyResolutionException e) {
      logger.warn("Dependencies couldn't be resolved for request '{}', {}", collectRequest, e.getMessage());
      node = e.getResult().getRoot();
      logUnresolvedArtifacts(node, e);
      throw e;
    }

    List<File> files = getFiles(node);
    return files;
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

  private RemoteRepository setAuthentication(RemoteRepository remoteRepository) {
    RemoteRepository.Builder authenticated = new RemoteRepository.Builder(remoteRepository);
    this.resolutionContext.getAuthenticatorSelector()
        .ifPresent(authSelector -> authenticated.setAuthentication(authSelector.getAuthentication(remoteRepository)));
    return authenticated.build();
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

    return nlg.getFiles().stream().map(File::getAbsoluteFile).collect(toList());
  }

  /**
   * Logs the paths for each dependency not found
   *
   * @param node root {@link DependencyNode}, can be a "null" root (imaginary root)
   * @param e {@link DependencyResolutionException} the error to collect paths.
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

}
