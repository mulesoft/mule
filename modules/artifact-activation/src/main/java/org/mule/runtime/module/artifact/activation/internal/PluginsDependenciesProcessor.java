/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorUtils.isCompatibleVersion;

import static java.util.Collections.synchronizedList;
import static java.util.Collections.synchronizedSet;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.module.artifact.activation.api.ArtifactActivationException;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.jgrapht.alg.TransitiveReduction;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import org.slf4j.Logger;

/**
 * Utility to process plugins in order according to their dependencies.
 *
 * @since 4.5
 */
public class PluginsDependenciesProcessor {

  private static final Logger LOGGER = getLogger(PluginsDependenciesProcessor.class);

  /**
   * Processes the given plugins in an ordered way.
   *
   * @param artifactPlugins plugin artifacts to be processed.
   * @param parallelize     whether the processing of each plugin dependencies can be parallelized.
   * @param processor       does the actual processing on the plugin.
   * @param <T>             generic type of the resulting object after plugin processing.
   *
   * @return {@link List} with the result of the plugins processing.
   */
  public static <T> List<T> process(List<ArtifactPluginDescriptor> artifactPlugins, boolean parallelize,
                                    BiConsumer<List<T>, ArtifactPluginDescriptor> processor) {
    final List<T> processedDependencies = synchronizedList(new ArrayList<>());

    SimpleDirectedGraph<BundleDescriptor, DefaultEdge> depsGraph = new SimpleDirectedGraph<>(DefaultEdge.class);

    artifactPlugins
        .stream()
        .forEach(apd -> depsGraph.addVertex(apd.getBundleDescriptor()));
    artifactPlugins
        .stream()
        .forEach(apd -> apd.getClassLoaderModel().getDependencies().stream()
            .filter(dep -> dep.getDescriptor().getClassifier().map(MULE_PLUGIN_CLASSIFIER::equals).orElse(false)
                // account for dependencies from parent artifact
                // TODO W-10927591 use the data form the extension model instead of assuming this (check with the failing test
                // when removing this condition)
                && depsGraph.containsVertex(dep.getDescriptor()))
            .forEach(dep -> depsGraph.addEdge(apd.getBundleDescriptor(), dep.getDescriptor(), new DefaultEdge())));
    TransitiveReduction.INSTANCE.reduce(depsGraph);

    LOGGER.debug("Dependencies graph: {}", depsGraph);

    while (!depsGraph.vertexSet().isEmpty()) {
      Set<BundleDescriptor> seenDependencies = synchronizedSet(new HashSet<>());

      artifactPluginsStream(artifactPlugins, parallelize)
          .filter(artifactPlugin -> depsGraph.vertexSet().contains(artifactPlugin.getBundleDescriptor())
              && depsGraph.outDegreeOf(artifactPlugin.getBundleDescriptor()) == 0)
          .forEach(artifactPlugin -> {
            LOGGER.debug("process({}): {}", parallelize ? "parallel" : "", artifactPlugin);

            // need this auxiliary structure because the graph does not support concurrent modifications
            seenDependencies.add(artifactPlugin.getBundleDescriptor());

            processor.accept(processedDependencies, artifactPlugin);
          });

      seenDependencies.forEach(depsGraph::removeVertex);
      LOGGER.debug("process({}): next iteration on the depsGraph...", parallelize ? "parallel" : "");
    }

    return processedDependencies;
  }

  private static Stream<ArtifactPluginDescriptor> artifactPluginsStream(List<ArtifactPluginDescriptor> artifactPlugins,
                                                                        boolean parallelize) {
    return parallelize ? artifactPlugins.parallelStream() : artifactPlugins.stream();
  }

  /**
   * Sanitizes the exported packages of the given plugins by removing the ones that are already being exported by their
   * transitive dependencies.
   *
   * @param artifactPlugins plugin artifacts whose exported packages are to be sanitized.
   * @return plugins with sanitized exported packages.
   */
  public static List<ArtifactPluginDescriptor> removeExportedPackagesAlreadyExportedByTransitiveDependencies(List<ArtifactPluginDescriptor> artifactPlugins) {
    List<ArtifactPluginDescriptor> resolvedPlugins = new LinkedList<>();
    List<ArtifactPluginDescriptor> unresolvedPlugins = new LinkedList<>(artifactPlugins);

    boolean continueResolution = true;

    while (continueResolution) {
      int initialResolvedCount = resolvedPlugins.size();

      List<ArtifactPluginDescriptor> pendingUnresolvedPlugins = new LinkedList<>();

      for (ArtifactPluginDescriptor unresolvedPlugin : unresolvedPlugins) {
        if (isResolvedPlugin(unresolvedPlugin, resolvedPlugins)) {
          sanitizeExportedPackages(unresolvedPlugin, resolvedPlugins);
          resolvedPlugins.add(unresolvedPlugin);
        } else {
          pendingUnresolvedPlugins.add(unresolvedPlugin);
        }
      }

      // Will try to resolve the plugins that are still unresolved
      unresolvedPlugins = pendingUnresolvedPlugins;

      continueResolution = resolvedPlugins.size() > initialResolvedCount;
    }

    if (unresolvedPlugins.size() != 0) {
      throw new ArtifactActivationException(createResolutionErrorMessage(unresolvedPlugins, resolvedPlugins));
    }

    return resolvedPlugins;
  }

  protected static I18nMessage createResolutionErrorMessage(List<ArtifactPluginDescriptor> unresolvedPlugins,
                                                            List<ArtifactPluginDescriptor> resolvedPlugins) {
    StringBuilder builder = new StringBuilder("Unable to resolve plugin dependencies:");
    for (ArtifactPluginDescriptor unresolvedPlugin : unresolvedPlugins) {
      builder.append("\nPlugin: ").append(unresolvedPlugin.getName()).append(" missing dependencies:");
      List<BundleDependency> missingDependencies = new ArrayList<>();
      for (BundleDependency dependency : unresolvedPlugin.getClassLoaderModel().getDependencies()) {
        Optional<String> classifierOptional = dependency.getDescriptor().getClassifier();
        if (classifierOptional.isPresent() && MULE_PLUGIN_CLASSIFIER.equals(classifierOptional.get())) {
          final ArtifactPluginDescriptor dependencyDescriptor = findArtifactPluginDescriptor(dependency, resolvedPlugins);
          if (dependencyDescriptor == null) {
            missingDependencies.add(dependency);
          }
        }
      }

      builder.append(missingDependencies);
    }

    return createStaticMessage(builder.toString());
  }

  private static void sanitizeExportedPackages(ArtifactPluginDescriptor pluginDescriptor,
                                               List<ArtifactPluginDescriptor> resolvedPlugins) {

    final Set<String> packagesExportedByDependencies =
        findDependencyPackageClosure(pluginDescriptor.getClassLoaderModel().getDependencies(), resolvedPlugins);

    ClassLoaderModel originalClassLoaderModel = pluginDescriptor.getClassLoaderModel();
    final Set<String> exportedClassPackages = new HashSet<>(originalClassLoaderModel.getExportedPackages());
    exportedClassPackages.removeAll(packagesExportedByDependencies);
    // TODO W-11203349 - check if the dependency belongs to the deny-list to decide whether to include local packages
    boolean includeLocals = true;
    pluginDescriptor.setClassLoaderModel(createBuilderWithoutExportedPackages(originalClassLoaderModel, includeLocals)
        .exportingPackages(exportedClassPackages).build());
  }

  private static ClassLoaderModel.ClassLoaderModelBuilder createBuilderWithoutExportedPackages(ClassLoaderModel originalClassLoaderModel,
                                                                                               boolean includeLocals) {
    ClassLoaderModel.ClassLoaderModelBuilder classLoaderModelBuilder = new ClassLoaderModel.ClassLoaderModelBuilder()
        .dependingOn(originalClassLoaderModel.getDependencies())
        .exportingPrivilegedPackages(originalClassLoaderModel.getPrivilegedExportedPackages(),
                                     originalClassLoaderModel.getPrivilegedArtifacts())
        .exportingResources(originalClassLoaderModel.getExportedResources());

    if (includeLocals) {
      classLoaderModelBuilder.withLocalResources(originalClassLoaderModel.getLocalResources())
          .withLocalPackages(originalClassLoaderModel.getLocalPackages());
    }

    for (URL url : originalClassLoaderModel.getUrls()) {
      classLoaderModelBuilder.containing(url);
    }
    return classLoaderModelBuilder;
  }

  private static Set<String> findDependencyPackageClosure(Set<BundleDependency> pluginDependencies,
                                                          List<ArtifactPluginDescriptor> resolvedPlugins) {
    Set<String> exportedPackages = new HashSet<>();
    for (BundleDependency pluginDependency : pluginDependencies) {
      final Optional<String> classifier = pluginDependency.getDescriptor().getClassifier();
      if (classifier.isPresent() && MULE_PLUGIN_CLASSIFIER.equals(classifier.get())) {
        ArtifactPluginDescriptor dependencyDescriptor = findArtifactPluginDescriptor(pluginDependency, resolvedPlugins);
        exportedPackages.addAll(dependencyDescriptor.getClassLoaderModel().getExportedPackages());
        exportedPackages
            .addAll(findDependencyPackageClosure(dependencyDescriptor.getClassLoaderModel().getDependencies(), resolvedPlugins));
      }
    }

    return exportedPackages;
  }

  private static boolean isResolvedPlugin(ArtifactPluginDescriptor descriptor, List<ArtifactPluginDescriptor> resolvedPlugins) {
    boolean isResolved = descriptor.getClassLoaderModel().getDependencies().isEmpty();

    if (!isResolved && hasPluginDependenciesResolved(descriptor.getClassLoaderModel().getDependencies(), resolvedPlugins)) {
      isResolved = true;
    }

    return isResolved;
  }

  private static boolean hasPluginDependenciesResolved(Set<BundleDependency> pluginDependencies,
                                                       List<ArtifactPluginDescriptor> resolvedPlugins) {
    boolean resolvedDependency = true;

    for (BundleDependency dependency : pluginDependencies) {
      if (dependency.getDescriptor().isPlugin()
          && findArtifactPluginDescriptor(dependency, resolvedPlugins) == null) {
        resolvedDependency = false;
        break;
      }
    }

    return resolvedDependency;
  }

  private static ArtifactPluginDescriptor findArtifactPluginDescriptor(BundleDependency bundleDependency,
                                                                       List<ArtifactPluginDescriptor> resolvedPlugins) {
    ArtifactPluginDescriptor result = null;

    for (ArtifactPluginDescriptor resolvedPlugin : resolvedPlugins) {
      BundleDescriptor resolvedBundleDescriptor = resolvedPlugin.getBundleDescriptor();
      if (isResolvedDependency(resolvedBundleDescriptor, bundleDependency.getDescriptor())) {
        result = resolvedPlugin;
        break;
      }
    }

    return result;
  }

  private static boolean isResolvedDependency(BundleDescriptor availableBundleDescriptor,
                                              BundleDescriptor expectedBundleDescriptor) {
    return availableBundleDescriptor.getArtifactId().equals(expectedBundleDescriptor.getArtifactId()) &&
        availableBundleDescriptor.getGroupId().equals(expectedBundleDescriptor.getGroupId()) &&
        isCompatibleVersion(availableBundleDescriptor.getVersion(), expectedBundleDescriptor.getVersion());
  }
}
