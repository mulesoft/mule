/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.plugin;

import static java.io.File.separator;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.maven.repository.internal.MavenRepositorySystemUtils.newSession;
import static org.eclipse.aether.repository.RepositoryPolicy.CHECKSUM_POLICY_IGNORE;
import static org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_NEVER;
import static org.eclipse.aether.util.artifact.ArtifactIdUtils.toId;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_ARTIFACT_FOLDER;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_POM;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.REPOSITORY;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.module.deployment.impl.internal.plugin.MavenUtils.getPomModel;
import org.apache.maven.model.Model;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
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
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
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
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.PatternInclusionsDependencyFilter;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;
import org.eclipse.aether.util.graph.visitor.PathRecordingDependencyVisitor;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants;
import org.mule.runtime.deployment.model.internal.plugin.BundlePluginDependenciesResolver;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.descriptor.BundleScope;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel.ClassLoaderModelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class is responsible of returning the {@link BundleDescriptor} of a given plugin's location and also creating a {@link ClassLoaderModel}
 * TODO(fernandezlautaro): MULE-11094 this class is the default implementation for discovering dependencies and URLs, which happens to be Maven based. There could be other ways to look for dependencies and URLs (probably for testing purposes where the plugins are done by hand and without maven) which will imply implementing the jira pointed out in this comment.
 *
 * @since 4.0
 */
public class MavenClassLoaderModelLoader {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  private DefaultRepositorySystemSession session;
  private RepositorySystem system;

  /**
   * Given a plugin's location, it will resolve its dependencies on a Maven based mechanism. It will assume there's a {@link ArtifactPluginDescriptor#REPOSITORY}
   * folder to look for the artifacts in it (which includes both JAR files as well as POM ones).
   * <p/>
   * It takes care of the transitive compile and runtime dependencies, from which will take the URLs to add them to the resulting
   * {@link ClassLoaderModel}, and it will also consume all Mule plugin dependencies so that further validations can check whether
   * or not all plugins are loaded in memory before running an application.
   * <p/>
   * Finally, it will also tell the resulting {@link ClassLoaderModel} which packages and/or resources has to export, consuming the
   * attributes from the {@link MuleArtifactLoaderDescriptor#getAttributes()} map.
   *
   * @param pluginFolder {@link File} where the current plugin to work with.
   * @param attributes a set of attributes to work with, where the current implementation of this class will look for {@link MavenClassLoaderConstants#EXPORTED_PACKAGES}
   *                   and {@link MavenClassLoaderConstants#EXPORTED_RESOURCES}
   * @return a {@link ClassLoaderModel} loaded with all its dependencies and URLs.
   * @see BundlePluginDependenciesResolver#getArtifactsWithDependencies(List, Set)
   */
  public ClassLoaderModel loadClassLoaderModel(File pluginFolder,
                                               Map<String, Object> attributes) {
    final Model model = getPomModel(pluginFolder);
    final ClassLoaderModelBuilder classLoaderModelBuilder = new ClassLoaderModelBuilder();
    classLoaderModelBuilder
        .exportingPackages(new HashSet<>(getAttribute(attributes, EXPORTED_PACKAGES)))
        .exportingResources(new HashSet<>(getAttribute(attributes, EXPORTED_RESOURCES)));
    final PreorderNodeListGenerator nlg = assemblyDependenciesFromPom(pluginFolder, model);
    loadUrls(pluginFolder, classLoaderModelBuilder, nlg);
    loadDependencies(classLoaderModelBuilder, nlg);
    return classLoaderModelBuilder.build();
  }

  private void loadDependencies(ClassLoaderModelBuilder classLoaderModelBuilder, PreorderNodeListGenerator nlg) {
    // Looking for all Mule plugin dependencies
    final Set<BundleDependency> plugins = new HashSet<>();
    nlg.getDependencies(true).stream()
        .filter(this::isMulePlugin)
        .map(Dependency::getArtifact)
        .forEach(artifact -> {
          final BundleDescriptor.Builder bundleDescriptorBuilder = new BundleDescriptor.Builder()
              .setArtifactId(artifact.getArtifactId())
              .setGroupId(artifact.getGroupId())
              .setVersion(artifact.getVersion())
              .setType(artifact.getExtension())
              .setClassifier(artifact.getClassifier());

          plugins.add(new BundleDependency.Builder()
              .setDescriptor(bundleDescriptorBuilder.build())
              .setScope(BundleScope.COMPILE)
              .build());
        });
    classLoaderModelBuilder.dependingOn(plugins);
  }

  private void loadUrls(File pluginFolder, ClassLoaderModelBuilder classLoaderModelBuilder,
                        PreorderNodeListGenerator nlg) {
    // Adding the exploded JAR root folder
    classLoaderModelBuilder.containing(getUrl(pluginFolder, pluginFolder));

    nlg.getArtifacts(false).stream().forEach(artifact -> {
      // Adding all needed jar's file dependencies
      classLoaderModelBuilder.containing(getUrl(pluginFolder, artifact.getFile()));
    });
  }

  /**
   * Dependency validator to keep those that are Mule plugins.
   * TODO(fernandezlautaro): MULE-11095 We will keep only Mule plugins dependencies or org.mule.runtime.deployment.model.internal.plugin.BundlePluginDependenciesResolver.getArtifactsWithDependencies() will fail looking them up.
   *
   * @param dependency to validate
   * @return true if the {@link Dependency} is {@link ArtifactPluginDescriptor#MULE_PLUGIN_CLASSIFIER}, false otherwise
   */
  private boolean isMulePlugin(Dependency dependency) {
    return BundleScope.PROVIDED.toString().equals(dependency.getScope().toUpperCase())
        && MULE_PLUGIN_CLASSIFIER.equals(dependency.getArtifact().getClassifier());
  }

  private PreorderNodeListGenerator assemblyDependenciesFromPom(File pluginFolder, Model model) {

    Artifact defaultArtifact = new DefaultArtifact(model.getGroupId(), model.getArtifactId(),
                                                   null,
                                                   "pom",
                                                   model.getVersion() != null ? model.getVersion()
                                                       : model.getParent().getVersion());
    createRepositorySystem(pluginFolder, defaultArtifact);
    final CollectRequest currentPluginRequest = new CollectRequest();
    try {
      final ArtifactDescriptorResult artifactDescriptorResult =
          system.readArtifactDescriptor(session, new ArtifactDescriptorRequest(defaultArtifact, null, null));
      currentPluginRequest.setDependencies(artifactDescriptorResult.getDependencies());
      currentPluginRequest.setManagedDependencies(artifactDescriptorResult.getManagedDependencies());

      final CollectResult collectResult = system.collectDependencies(session, currentPluginRequest);

      final DependencyRequest currentPluginDependenciesRequest = new DependencyRequest();
      currentPluginDependenciesRequest.setFilter(new ScopeDependencyFilter(JavaScopes.TEST, JavaScopes.PROVIDED));
      currentPluginDependenciesRequest.setRoot(collectResult.getRoot());
      currentPluginDependenciesRequest.setCollectRequest(currentPluginRequest);
      final DependencyResult dependencyResult = system.resolveDependencies(session, currentPluginDependenciesRequest);
      final PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
      dependencyResult.getRoot().accept(nlg);
      return nlg;
    } catch (DependencyResolutionException e) {
      DependencyNode node = e.getResult().getRoot();
      logUnresolvedArtifacts(node, e);
      throw new ArtifactDescriptorCreateException(format("There was an issue solving the dependencies for the plugin [%s]",
                                                         pluginFolder.getAbsolutePath()),
                                                  e);
    } catch (DependencyCollectionException e) {
      throw new ArtifactDescriptorCreateException(format("There was an issue resolving the dependency tree for the plugin [%s]",
                                                         pluginFolder.getAbsolutePath()),
                                                  e);
    } catch (ArtifactDescriptorException e) {
      throw new ArtifactDescriptorCreateException(format("There was an issue resolving the artifact descriptor for the plugin [%s]",
                                                         pluginFolder.getAbsolutePath()),
                                                  e);
    }
  }

  private URL getUrl(File pluginFolder, File file) {
    try {
      return file.toURI().toURL();
    } catch (MalformedURLException e) {
      throw new ArtifactDescriptorCreateException(format("There was an issue obtaining the URL for the plugin [%s], file [%s]",
                                                         pluginFolder.getAbsolutePath(), file.getAbsolutePath()),
                                                  e);
    }
  }

  private List<String> getAttribute(Map<String, Object> attributes, String attribute) {
    final Object attributeObject = attributes.getOrDefault(attribute, new ArrayList<String>());
    checkArgument(attributeObject instanceof List, format("The '%s' attribute must be of '%s', found '%s'", attribute,
                                                          List.class.getName(), attributeObject.getClass().getName()));
    return (List<String>) attributeObject;
  }

  private void createRepositorySystem(File pluginFolder, Artifact pluginArtifact) {

    session = newDefaultRepositorySystemSession();
    session.setOffline(true);
    session.setIgnoreArtifactDescriptorRepositories(true);
    session.setWorkspaceReader(new PomWorkspaceReader(pluginFolder, pluginArtifact));

    File mavenLocalRepositoryLocation = new File(pluginFolder, REPOSITORY);
    system = newRepositorySystem(mavenLocalRepositoryLocation, session);
  }

  private static DefaultRepositorySystemSession newDefaultRepositorySystemSession() {
    final DefaultRepositorySystemSession session = newSession();
    session.setUpdatePolicy(UPDATE_POLICY_NEVER);
    session.setChecksumPolicy(CHECKSUM_POLICY_IGNORE);
    return session;
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
   * Custom implementation of a {@link WorkspaceReader} meant to be tightly used with the plugin mechanism, where the POM file is
   * inside the {@link ArtifactPluginDescriptor#MULE_ARTIFACT_FOLDER}. For any other {@link Artifact} it will return values that will force the
   * dependency mechanism to look for in a different {@link WorkspaceReader}
   *
   * @since 4.0
   */
  private class PomWorkspaceReader implements WorkspaceReader {

    private final File pluginFolder;
    private final Artifact pluginArtifact;
    final WorkspaceRepository workspaceRepository;

    /**
     * @param pluginFolder plugin's folder used to look for the POM file
     * @param pluginArtifact plugin's artifact to compare, so that resolves the file in {@link #findArtifact(Artifact)} when it matches
     *                       with the {@link #pluginArtifact}
     */
    PomWorkspaceReader(File pluginFolder, Artifact pluginArtifact) {
      this.pluginFolder = pluginFolder;
      this.pluginArtifact = pluginArtifact;
      this.workspaceRepository = new WorkspaceRepository(format("worskpace-repository-%s", pluginFolder.getName()));
    }

    @Override
    public WorkspaceRepository getRepository() {
      return workspaceRepository;
    }

    @Override
    public File findArtifact(Artifact artifact) {
      if (checkArtifact(artifact)) {
        return new File(pluginFolder, MULE_ARTIFACT_FOLDER + separator + MULE_PLUGIN_POM);
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
