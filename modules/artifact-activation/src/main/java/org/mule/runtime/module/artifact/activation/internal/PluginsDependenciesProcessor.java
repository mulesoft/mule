/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal;

import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;

import static java.util.Collections.synchronizedList;
import static java.util.Collections.synchronizedSet;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
  public static <T> List<T> process(Collection<ArtifactPluginDescriptor> artifactPlugins, boolean parallelize,
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

  private static Stream<ArtifactPluginDescriptor> artifactPluginsStream(Collection<ArtifactPluginDescriptor> artifactPlugins,
                                                                        boolean parallelize) {
    return parallelize ? artifactPlugins.parallelStream() : artifactPlugins.stream();
  }

}
