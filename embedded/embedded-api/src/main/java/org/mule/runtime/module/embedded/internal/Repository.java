/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.embedded.internal;

import static java.lang.System.getProperty;
import static java.util.stream.Collectors.toList;
import static org.eclipse.aether.repository.RepositoryPolicy.CHECKSUM_POLICY_IGNORE;
import static org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_NEVER;
import static org.eclipse.aether.util.artifact.ArtifactIdUtils.toId;
import static org.eclipse.aether.util.artifact.JavaScopes.COMPILE;
import static org.eclipse.aether.util.artifact.JavaScopes.PROVIDED;
import static org.eclipse.aether.util.artifact.JavaScopes.RUNTIME;
import static org.eclipse.aether.util.artifact.JavaScopes.SYSTEM;
import static org.eclipse.aether.util.artifact.JavaScopes.TEST;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
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
import org.eclipse.aether.util.filter.PatternInclusionsDependencyFilter;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.eclipse.aether.util.graph.visitor.PathRecordingDependencyVisitor;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to work with a maven repository.
 *
 * @since 4.0
 */
// TODO MULE-11878 - consolidate with other aether usages in mule.
public class Repository {

  private static final String USER_HOME = "user.home";
  private static final String M2_REPO = "/.m2/repository";
  public static final String MULE_SERVICE = "mule-service";
  private static String userHome = getProperty(USER_HOME);
  private static final String MAVEN_REPOSITORY_FOLDER = userHome + M2_REPO;

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  private DefaultRepositorySystemSession session;
  private RepositorySystem system;

  public Repository() {
    createRepositorySystem();
  }

  private void createRepositorySystem() {
    session = newDefaultRepositorySystemSession();
    session.setOffline(true);
    session.setIgnoreArtifactDescriptorRepositories(true);

    File mavenLocalRepositoryLocation = new File(MAVEN_REPOSITORY_FOLDER);
    system = newRepositorySystem(mavenLocalRepositoryLocation, session);
  }

  private static DefaultRepositorySystemSession newDefaultRepositorySystemSession() {
    final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    session.setUpdatePolicy(UPDATE_POLICY_NEVER);
    session.setChecksumPolicy(CHECKSUM_POLICY_IGNORE);

    DependencySelector dependencySelector =
        new AndDependencySelector(new ScopeDependencySelector(RUNTIME, TEST),
                                  new OptionalDependencySelector(),
                                  new ExclusionDependencySelector());
    session.setDependencySelector(dependencySelector);

    return session;
  }

  public PreorderNodeListGenerator assemblyDependenciesForArtifact(Artifact artifact, Predicate<Dependency> filter) {
    final CollectRequest collectRequest = new CollectRequest();
    try {
      final ArtifactDescriptorResult artifactDescriptorResult =
          system.readArtifactDescriptor(session,
                                        new ArtifactDescriptorRequest(artifact, null, null));
      collectRequest.setDependencies(artifactDescriptorResult.getDependencies()
          .stream()
          .filter(dependency -> filter.test(dependency))
          .collect(toList()));
      collectRequest.setManagedDependencies(artifactDescriptorResult.getManagedDependencies());

      final CollectResult collectResult = system.collectDependencies(session, collectRequest);

      final DependencyRequest dependencyRequest = new DependencyRequest();

      Collection<String> excluded = new ArrayList<>();
      Collections.addAll(excluded, PROVIDED, SYSTEM, RUNTIME, TEST);
      Collection<String> included = new ArrayList<>();
      included.add(COMPILE);
      dependencyRequest.setFilter(new ScopeDependencyFilter(included, excluded));

      dependencyRequest.setRoot(collectResult.getRoot());
      dependencyRequest.setCollectRequest(collectRequest);
      final DependencyResult dependencyResult =
          system.resolveDependencies(session, dependencyRequest);
      final PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
      dependencyResult.getRoot().accept(nlg);
      return nlg;
    } catch (DependencyResolutionException e) {
      DependencyNode node = e.getResult().getRoot();
      logUnresolvedArtifacts(node, e);
      throw new RuntimeException(
                                 String.format("There was an issue solving the dependencies for the container [%s]",
                                               artifact),
                                 e);
    } catch (DependencyCollectionException e) {
      throw new RuntimeException(
                                 String.format("There was an issue resolving the dependency tree for the container [%s]",
                                               artifact),
                                 e);
    } catch (ArtifactDescriptorException e) {
      throw new RuntimeException(
                                 String.format("There was an issue resolving the artifact descriptor for the container [%s]",
                                               artifact),
                                 e);
    }
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

  private static RepositorySystem newRepositorySystem(File mavenLocalRepositoryLocation, DefaultRepositorySystemSession session) {
    final RepositorySystem system = newRepositorySystem();
    // We have to set to use a "simple" aether local repository so it will not cache artifacts (enhanced is supported for doing
    // operations such install).
    final LocalRepository localRepo = new LocalRepository(mavenLocalRepositoryLocation, "simple");
    session
        .setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
    return system;
  }

  /**
   * Creates and configures the {@link RepositorySystem} to use for resolving transitive dependencies.
   *
   * @return {@link RepositorySystem}
   */
  private static RepositorySystem newRepositorySystem() {
    /*
     * Aether's components implement org.eclipse.aether.spi.locator.Service to ease manual wiring and using the pre populated
     * DefaultServiceLocator, we only MavenXpp3Reader need to register the repository connector and transporter factories.
     */
    final DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
    locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
    locator.addService(TransporterFactory.class, FileTransporterFactory.class);
    return locator.getService(RepositorySystem.class);
  }

  public DefaultRepositorySystemSession getSession() {
    return session;
  }

  public RepositorySystem getSystem() {
    return system;
  }
}
