/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.plugin;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.artifact.activation.internal.plugin.PluginLocalDependenciesDenylist.isDenylisted;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorUtils.isCompatibleVersion;

import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.Comparator.comparing;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toSet;

import org.mule.runtime.module.artifact.activation.api.ArtifactActivationException;
import org.mule.runtime.module.artifact.activation.api.plugin.DuplicateExportedPackageException;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginResolutionError;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration.ClassLoaderConfigurationBuilder;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves plugin dependencies considering the plugin name only.
 */
public class BundlePluginDependenciesResolver {

  private static final Logger logger = LoggerFactory.getLogger(BundlePluginDependenciesResolver.class);
  protected static final String MULE_HTTP_CONNECTOR_ARTIFACT_ID = "mule-http-connector";
  protected static final String MULE_HTTP_CONNECTOR_GROUP_ID = "org.mule.connectors";

  /**
   * Resolves the dependencies between a group of plugins. It sorts them in a lexicographic order by name to then sanitize the
   * packages exported by the plugins dependencies (avoids exporting elements that are already exported by other plugin).
   *
   * @param providedPluginDescriptors plugins descriptors provided by a parent Mule artifact if it exists.
   * @param descriptors               plugins descriptors to resolve.
   * @param isDomain                  {@code true} if {@code providedPluginDescriptors} come from a {@code domain}, false
   *                                  otherwise.
   * @return a non-null list containing the plugins in resolved order.
   * @throws PluginResolutionError if at least a plugin cannot be resolved.
   */
  public List<ArtifactPluginDescriptor> resolve(Set<ArtifactPluginDescriptor> providedPluginDescriptors,
                                                List<ArtifactPluginDescriptor> descriptors, boolean isDomain)
      throws PluginResolutionError {

    List<ArtifactPluginDescriptor> resolvedPlugins = resolvePluginsDependencies(descriptors);

    List<ArtifactPluginDescriptor> filteredPluginDescriptors =
        getArtifactPluginDescriptors(providedPluginDescriptors, resolvedPlugins, isDomain);

    verifyPluginExportedPackages(filteredPluginDescriptors);

    return filteredPluginDescriptors;
  }

  private List<ArtifactPluginDescriptor> getArtifactPluginDescriptors(Set<ArtifactPluginDescriptor> domainPlugins,
                                                                      List<ArtifactPluginDescriptor> resolvedPlugins,
                                                                      boolean isDomain) {
    List<ArtifactPluginDescriptor> filteredPluginDescriptors = new ArrayList<>();

    for (ArtifactPluginDescriptor appPluginDescriptor : resolvedPlugins) {
      Optional<ArtifactPluginDescriptor> pluginDescriptor = findPlugin(domainPlugins, appPluginDescriptor.getBundleDescriptor());

      if (!pluginDescriptor.isPresent()) {
        filteredPluginDescriptors.add(appPluginDescriptor);
      } else {
        BundleDescriptor foundPluginBundleDescriptor = pluginDescriptor.get().getBundleDescriptor();
        // TODO MULE-15842: remove hardcoded HTTP artifact GAs.
        if ((isDomain || (foundPluginBundleDescriptor.getArtifactId().equals(MULE_HTTP_CONNECTOR_ARTIFACT_ID) &&
            foundPluginBundleDescriptor.getGroupId().equals(MULE_HTTP_CONNECTOR_GROUP_ID)))
            && !isCompatibleVersion(foundPluginBundleDescriptor.getVersion(),
                                    appPluginDescriptor.getBundleDescriptor().getVersion())) {
          throw new IllegalStateException(
                                          format("Incompatible version of plugin '%s' (%s:%s) found. Artifact requires version '%s' but context provides version '%s'",
                                                 appPluginDescriptor.getName(),
                                                 appPluginDescriptor.getBundleDescriptor().getGroupId(),
                                                 appPluginDescriptor.getBundleDescriptor().getArtifactId(),
                                                 appPluginDescriptor.getBundleDescriptor().getVersion(),
                                                 foundPluginBundleDescriptor.getVersion()));
        }
      }
    }
    return filteredPluginDescriptors;
  }

  private Optional<ArtifactPluginDescriptor> findPlugin(Collection<ArtifactPluginDescriptor> appPlugins,
                                                        BundleDescriptor bundleDescriptor) {
    for (ArtifactPluginDescriptor appPlugin : appPlugins) {
      if (appPlugin.getBundleDescriptor().getArtifactId().equals(bundleDescriptor.getArtifactId())
          && appPlugin.getBundleDescriptor().getGroupId().equals(bundleDescriptor.getGroupId())) {
        return of(appPlugin);
      }
    }

    return empty();
  }

  private List<ArtifactPluginDescriptor> resolvePluginsDependencies(List<ArtifactPluginDescriptor> descriptors) {
    Set<BundleDescriptor> knownPlugins =
        descriptors.stream().map(ArtifactPluginDescriptor::getBundleDescriptor).collect(toSet());
    descriptors = getArtifactsWithDependencies(descriptors, knownPlugins);

    List<ArtifactPluginDescriptor> sortedDescriptors = new ArrayList<>(descriptors);
    sortedDescriptors.sort(comparing(ArtifactDescriptor::getName));

    List<ArtifactPluginDescriptor> resolvedPlugins = new LinkedList<>();
    List<ArtifactPluginDescriptor> unresolvedPlugins = new LinkedList<>(sortedDescriptors);

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
      throw new PluginResolutionError(createResolutionErrorMessage(unresolvedPlugins, resolvedPlugins));
    }

    return resolvedPlugins;
  }

  private void verifyPluginExportedPackages(List<ArtifactPluginDescriptor> plugins) {
    final Map<String, List<String>> exportedPackages = new HashMap<>();

    boolean error = false;
    for (ArtifactPluginDescriptor plugin : plugins) {
      for (String packageName : plugin.getClassLoaderConfiguration().getExportedPackages()) {
        List<String> exportedOn = exportedPackages.get(packageName);

        if (exportedOn == null) {
          exportedOn = new LinkedList<>();
          exportedPackages.put(packageName, exportedOn);
        } else {
          error = true;
        }
        exportedOn.add(plugin.getName());
      }
    }

    if (error) {
      throw new DuplicateExportedPackageException(exportedPackages);
    }
  }

  /**
   * Goes over the elements in the {@code pluginDescriptors} collection looking if it hasn't been resolved yet.
   *
   * @param pluginDescriptors plugins to validate.
   * @param visited           plugins that are already resolved (by either the container or application initially, or by the
   *                          resolver).
   * @return the plugins that were obtained initially plus all the ones that were found.
   */
  private List<ArtifactPluginDescriptor> getArtifactsWithDependencies(List<ArtifactPluginDescriptor> pluginDescriptors,
                                                                      Set<BundleDescriptor> visited) {
    List<ArtifactPluginDescriptor> pluginDescriptorsWithDependences = new ArrayList<>();
    pluginDescriptorsWithDependences.addAll(pluginDescriptors);

    if (!pluginDescriptors.isEmpty()) {
      List<ArtifactPluginDescriptor> foundDependencies = new ArrayList<>();
      pluginDescriptors.stream()
          .filter(pluginDescriptor -> pluginDescriptor.getBundleDescriptor().isPlugin())
          .forEach(pluginDescriptor -> pluginDescriptor.getClassLoaderConfiguration().getDependencies()
              .forEach(dependency -> {
                Optional<ArtifactPluginDescriptor> resolvedPluginApplicationLevelOptional =
                    findPlugin(pluginDescriptorsWithDependences, dependency.getDescriptor());
                if (isPlugin(dependency) && !isResolvedDependency(visited, dependency.getDescriptor())) {
                  if (dependency.getBundleUri() == null) {
                    throw new PluginResolutionError(format("Bundle URL should have been resolved for %s.",
                                                           dependency.getDescriptor()));
                  }

                  if (resolvedPluginApplicationLevelOptional.isPresent()) {
                    ArtifactPluginDescriptor artifactPluginDescriptorResolved = resolvedPluginApplicationLevelOptional.get();
                    logger
                        .warn(format("Transitive plugin dependency '[%s -> %s]' is greater than the one resolved for the application '%s', it will be ignored.",
                                     pluginDescriptor.getBundleDescriptor(), dependency.getDescriptor(),
                                     artifactPluginDescriptorResolved.getBundleDescriptor()));
                    ClassLoaderConfiguration originalClassLoaderConfiguration = pluginDescriptor.getClassLoaderConfiguration();
                    boolean includeLocals = !isDenylisted(pluginDescriptor.getBundleDescriptor());
                    pluginDescriptor
                        .setClassLoaderConfiguration(createBuilderWithoutDependency(originalClassLoaderConfiguration, dependency,
                                                                                    includeLocals)
                            .dependingOn(singleton(new BundleDependency.Builder()
                                .setDescriptor(artifactPluginDescriptorResolved
                                    .getBundleDescriptor())
                                .setScope(dependency
                                    .getScope())
                                .build()))
                            .build());
                  } else {
                    throw new ArtifactActivationException(createStaticMessage(format("Transitive dependencies should be resolved by now for mule-plugin '%s', but '%s' is missing",
                                                                                     pluginDescriptor.getBundleDescriptor(),
                                                                                     dependency.getDescriptor())));
                  }
                } else {
                  if (resolvedPluginApplicationLevelOptional.isPresent()) {
                    BundleDescriptor availablePluginBundleDescriptor =
                        resolvedPluginApplicationLevelOptional.get().getBundleDescriptor();
                    if (org.apache.commons.lang3.ObjectUtils.notEqual(availablePluginBundleDescriptor.getVersion(),
                                                                      dependency.getDescriptor().getVersion())) {
                      if (logger.isDebugEnabled()) {
                        logger.debug(format(
                                            "Transitive plugin dependency '[%s -> %s]' is minor than the one resolved for the application '%s', it will be ignored.",
                                            pluginDescriptor.getBundleDescriptor(), dependency.getDescriptor(),
                                            availablePluginBundleDescriptor));
                      }
                    }
                  }
                }
              }));

      pluginDescriptorsWithDependences.addAll(getArtifactsWithDependencies(foundDependencies, visited));
    }
    return pluginDescriptorsWithDependences;
  }

  private boolean isPlugin(BundleDependency dependency) {
    return dependency.getDescriptor().isPlugin();
  }

  private void sanitizeExportedPackages(ArtifactPluginDescriptor pluginDescriptor,
                                        List<ArtifactPluginDescriptor> resolvedPlugins) {

    final Set<String> packagesExportedByDependencies =
        findDependencyPackageClosure(pluginDescriptor.getClassLoaderConfiguration().getDependencies(), resolvedPlugins);

    ClassLoaderConfiguration originalClassLoaderConfiguration = pluginDescriptor.getClassLoaderConfiguration();
    final Set<String> exportedClassPackages = new HashSet<>(originalClassLoaderConfiguration.getExportedPackages());
    exportedClassPackages.removeAll(packagesExportedByDependencies);
    boolean includeLocals = !isDenylisted(pluginDescriptor.getBundleDescriptor());
    pluginDescriptor
        .setClassLoaderConfiguration(createBuilderWithoutExportedPackages(originalClassLoaderConfiguration, includeLocals)
            .exportingPackages(exportedClassPackages).build());
  }

  private ClassLoaderConfigurationBuilder createBuilderWithoutExportedPackages(ClassLoaderConfiguration originalClassLoaderConfiguration,
                                                                               boolean includeLocals) {
    ClassLoaderConfigurationBuilder classLoaderConfigurationBuilder = new ClassLoaderConfigurationBuilder()
        .dependingOn(originalClassLoaderConfiguration.getDependencies())
        .exportingPrivilegedPackages(originalClassLoaderConfiguration.getPrivilegedExportedPackages(),
                                     originalClassLoaderConfiguration.getPrivilegedArtifacts())
        .exportingResources(originalClassLoaderConfiguration.getExportedResources());

    if (includeLocals) {
      classLoaderConfigurationBuilder.withLocalResources(originalClassLoaderConfiguration.getLocalResources())
          .withLocalPackages(originalClassLoaderConfiguration.getLocalPackages());
    }

    for (URL url : originalClassLoaderConfiguration.getUrls()) {
      classLoaderConfigurationBuilder.containing(url);
    }
    return classLoaderConfigurationBuilder;
  }

  private ClassLoaderConfigurationBuilder createBuilderWithoutDependency(ClassLoaderConfiguration originalClassLoaderConfiguration,
                                                                         BundleDependency dependencyToBeExcluded,
                                                                         boolean includeLocals) {
    ClassLoaderConfigurationBuilder classLoaderConfigurationBuilder = new ClassLoaderConfigurationBuilder()
        .dependingOn(originalClassLoaderConfiguration.getDependencies().stream()
            .filter(dependency -> !dependency.equals(dependencyToBeExcluded))
            .collect(toSet()))
        .exportingPackages(originalClassLoaderConfiguration.getExportedPackages())
        .exportingPrivilegedPackages(originalClassLoaderConfiguration.getPrivilegedExportedPackages(),
                                     originalClassLoaderConfiguration.getPrivilegedArtifacts())
        .exportingResources(originalClassLoaderConfiguration.getExportedResources());

    if (includeLocals) {
      classLoaderConfigurationBuilder.withLocalResources(originalClassLoaderConfiguration.getLocalResources())
          .withLocalPackages(originalClassLoaderConfiguration.getLocalPackages());
    }

    for (URL url : originalClassLoaderConfiguration.getUrls()) {
      classLoaderConfigurationBuilder.containing(url);
    }
    return classLoaderConfigurationBuilder;
  }

  private Set<String> findDependencyPackageClosure(Set<BundleDependency> pluginDependencies,
                                                   List<ArtifactPluginDescriptor> resolvedPlugins) {
    Set<String> exportedPackages = new HashSet<>();
    for (BundleDependency pluginDependency : pluginDependencies) {
      final Optional<String> classifier = pluginDependency.getDescriptor().getClassifier();
      if (classifier.isPresent() && MULE_PLUGIN_CLASSIFIER.equals(classifier.get())) {
        ArtifactPluginDescriptor dependencyDescriptor = findArtifactPluginDescriptor(pluginDependency, resolvedPlugins);
        exportedPackages.addAll(dependencyDescriptor.getClassLoaderConfiguration().getExportedPackages());
        exportedPackages
            .addAll(findDependencyPackageClosure(dependencyDescriptor.getClassLoaderConfiguration().getDependencies(),
                                                 resolvedPlugins));
      }
    }

    return exportedPackages;
  }

  protected static String createResolutionErrorMessage(List<ArtifactPluginDescriptor> unresolvedPlugins,
                                                       List<ArtifactPluginDescriptor> resolvedPlugins) {
    StringBuilder builder = new StringBuilder("Unable to resolve plugin dependencies:");
    for (ArtifactPluginDescriptor unresolvedPlugin : unresolvedPlugins) {
      builder.append("\nPlugin: ").append(unresolvedPlugin.getName()).append(" missing dependencies:");
      List<BundleDependency> missingDependencies = new ArrayList<>();
      for (BundleDependency dependency : unresolvedPlugin.getClassLoaderConfiguration().getDependencies()) {
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

    return builder.toString();
  }

  private boolean isResolvedDependency(Set<BundleDescriptor> visited, BundleDescriptor descriptor) {
    for (BundleDescriptor resolvedDependency : visited) {
      if (isResolvedDependency(resolvedDependency, descriptor)) {
        return true;
      }
    }

    return false;
  }

  private boolean isResolvedPlugin(ArtifactPluginDescriptor descriptor, List<ArtifactPluginDescriptor> resolvedPlugins) {
    boolean isResolved = descriptor.getClassLoaderConfiguration().getDependencies().isEmpty();

    if (!isResolved
        && hasPluginDependenciesResolved(descriptor.getClassLoaderConfiguration().getDependencies(), resolvedPlugins)) {
      isResolved = true;
    }

    return isResolved;
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

  private boolean hasPluginDependenciesResolved(Set<BundleDependency> pluginDependencies,
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
}

