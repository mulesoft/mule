/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.maven;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.artifact.activation.internal.classloader.Classifier.MULE_PLUGIN;
import static org.mule.tools.api.classloader.model.ArtifactCoordinates.DEFAULT_ARTIFACT_TYPE;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

import static com.vdurmont.semver4j.Semver.SemverType.LOOSE;
import static org.apache.commons.io.FileUtils.toFile;

import org.mule.maven.client.api.model.BundleDependency;
import org.mule.maven.client.api.model.BundleDescriptor;
import org.mule.maven.client.internal.AetherMavenClient;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.module.artifact.activation.internal.classloader.model.utils.ArtifactUtils;
import org.mule.runtime.module.artifact.activation.internal.plugin.Plugin;
import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.vdurmont.semver4j.Semver;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.xml.Xpp3Dom;


/**
 * Resolves additional plugin libraries for all plugins declared.
 */
public class AdditionalPluginDependenciesResolver {

  protected static final String MULE_EXTENSIONS_PLUGIN_GROUP_ID = "org.mule.runtime.plugins";
  protected static final String MULE_EXTENSIONS_PLUGIN_ARTIFACT_ID = "mule-extensions-maven-plugin";
  protected static final String MULE_MAVEN_PLUGIN_GROUP_ID = "org.mule.tools.maven";
  protected static final String MULE_MAVEN_PLUGIN_ARTIFACT_ID = "mule-maven-plugin";
  protected static final String ADDITIONAL_PLUGIN_DEPENDENCIES_ELEMENT = "additionalPluginDependencies";
  protected static final String ADDITIONAL_DEPENDENCIES_ELEMENT = "additionalDependencies";
  protected static final String GROUP_ID_ELEMENT = "groupId";
  protected static final String ARTIFACT_ID_ELEMENT = "artifactId";
  protected static final String VERSION_ELEMENT = "version";
  protected static final String PLUGIN_ELEMENT = "plugin";
  protected static final String DEPENDENCY_ELEMENT = "dependency";
  private final AetherMavenClient aetherMavenClient;
  private final List<Plugin> pluginsWithAdditionalDependencies;
  private final File temporaryFolder;
  private final Map<ArtifactCoordinates, Supplier<Model>> pomModels;

  public AdditionalPluginDependenciesResolver(AetherMavenClient muleMavenPluginClient,
                                              List<Plugin> additionalPluginDependencies,
                                              File temporaryFolder,
                                              Map<ArtifactCoordinates, Supplier<Model>> pomModels) {
    this.aetherMavenClient = muleMavenPluginClient;
    this.pluginsWithAdditionalDependencies = new ArrayList<>(additionalPluginDependencies);
    this.temporaryFolder = temporaryFolder;
    this.pomModels = pomModels;
  }

  public Map<BundleDependency, List<BundleDependency>> resolveDependencies(List<BundleDependency> applicationDependencies,
                                                                           Map<ArtifactCoordinates, List<Artifact>> pluginsDependencies) {
    // addPluginDependenciesAdditionalLibraries(applicationDependencies);
    Map<BundleDependency, List<BundleDependency>> pluginsWithAdditionalDeps = new LinkedHashMap<>();
    for (Plugin pluginWithAdditionalDependencies : pluginsWithAdditionalDependencies) {
      BundleDependency pluginBundleDependency =
          getPluginBundleDependency(pluginWithAdditionalDependencies, applicationDependencies);
      List<Artifact> pluginDependencies =
          getPluginDependencies(pluginWithAdditionalDependencies, pluginsDependencies);
      List<BundleDependency> additionalDependencies =
          resolveDependencies(pluginWithAdditionalDependencies.getAdditionalDependencies().stream()
              .filter(additionalDep -> pluginDependencies.stream()
                  .noneMatch(artifactDependency -> areSameArtifact(additionalDep, artifactDependency)))
              .collect(toList()));
      if (!additionalDependencies.isEmpty()) {
        pluginsWithAdditionalDeps.put(pluginBundleDependency,
                                      additionalDependencies);
      }
    }
    return pluginsWithAdditionalDeps;
  }

  private List<BundleDependency> resolveDependencies(List<Dependency> additionalDependencies) {
    return aetherMavenClient.resolveArtifactDependencies(additionalDependencies.stream()
        .map(ArtifactUtils::toBundleDescriptor)
        .collect(toList()),
                                                         of(aetherMavenClient.getMavenConfiguration()
                                                             .getLocalMavenRepositoryLocation()),
                                                         empty());
  }

  private BundleDependency getPluginBundleDependency(Plugin plugin, List<BundleDependency> mulePlugins) {
    return mulePlugins.stream()
        .filter(mulePlugin -> StringUtils.equals(mulePlugin.getDescriptor().getArtifactId(), plugin.getArtifactId())
            && StringUtils.equals(mulePlugin.getDescriptor().getGroupId(), plugin.getGroupId()))
        .findFirst()
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Declared additional dependencies for a plugin not present: "
            + plugin)));
  }

  private List<Artifact> getPluginDependencies(Plugin plugin, Map<ArtifactCoordinates, List<Artifact>> pluginsDependencies) {
    return pluginsDependencies.entrySet().stream().filter(
                                                          pluginDependenciesEntry -> StringUtils
                                                              .equals(pluginDependenciesEntry.getKey().getGroupId(),
                                                                      plugin.getGroupId())
                                                              && StringUtils.equals(pluginDependenciesEntry.getKey()
                                                                  .getArtifactId(), plugin.getArtifactId()))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Could not find dependencies for plugin: "
            + plugin)));
  }

  private boolean areSameArtifact(Dependency dependency, Artifact artifact) {
    return StringUtils.equals(dependency.getArtifactId(), artifact.getArtifactCoordinates().getArtifactId())
        && StringUtils.equals(dependency.getGroupId(), artifact.getArtifactCoordinates().getGroupId())
        && StringUtils.equals(dependency.getVersion(), artifact.getArtifactCoordinates().getVersion());
  }

  private String getChildParameterValue(Xpp3Dom element, String childName, boolean validate) {
    Xpp3Dom child = element.getChild(childName);
    String childValue = child != null ? child.getValue() : null;
    if (StringUtils.isEmpty(childValue) && validate) {
      throw new IllegalArgumentException("Expecting child element with not null value " + childName);
    }
    return childValue;
  }

  private void addPluginDependenciesAdditionalLibraries(List<BundleDependency> applicationDependencies) {
    List<BundleDependency> mulePlugins = applicationDependencies
        .stream()
        .filter(bundleDependency -> MULE_PLUGIN
            .equals(bundleDependency.getDescriptor().getClassifier().orElse(null)))
        .collect(toList());

    Collection<Plugin> additionalDependenciesFromMulePlugins = resolveAdditionalDependenciesFromMulePlugins(mulePlugins);

    pluginsWithAdditionalDependencies.addAll(additionalDependenciesFromMulePlugins.stream()
        .filter(isNotRedefinedAtApplicationLevel())
        .collect(toList()));
  }

  protected Collection<Plugin> resolveAdditionalDependenciesFromMulePlugins(List<BundleDependency> mulePlugins) {
    Map<String, Plugin> additionalDependenciesFromMulePlugins = new HashMap<>();

    mulePlugins.forEach(mulePlugin -> {
      try {

        BundleDescriptor descriptor = mulePlugin.getDescriptor();
        ArtifactCoordinates artifactCoordinates =
            new ArtifactCoordinates(descriptor.getGroupId(), descriptor.getArtifactId(), descriptor.getVersion());
        URL mulePluginUrl = mulePlugin.getBundleUri().toURL();

        Supplier<Model> pomModel =
            pomModels.getOrDefault(artifactCoordinates, () -> aetherMavenClient.getEffectiveModel(toFile(mulePluginUrl),
                                                                                                  of(temporaryFolder)));

        Build build = pomModel.get().getBuild();
        if (build != null) {
          org.apache.maven.model.Plugin packagerPlugin =
              build.getPluginsAsMap().get(MULE_EXTENSIONS_PLUGIN_GROUP_ID + ":" + MULE_EXTENSIONS_PLUGIN_ARTIFACT_ID);
          if (packagerPlugin == null) {
            packagerPlugin =
                build.getPluginsAsMap().get(MULE_MAVEN_PLUGIN_GROUP_ID + ":" + MULE_MAVEN_PLUGIN_ARTIFACT_ID);
          }
          if (packagerPlugin != null) {
            Object configurationObject =
                packagerPlugin.getConfiguration();
            if (configurationObject != null) {
              Xpp3Dom additionalPluginDependenciesDom = ((Xpp3Dom) configurationObject)
                  .getChild(ADDITIONAL_PLUGIN_DEPENDENCIES_ELEMENT);
              if (additionalPluginDependenciesDom != null) {
                Xpp3Dom[] additionalPluginDependencies =
                    additionalPluginDependenciesDom.getChildren(PLUGIN_ELEMENT);
                if (additionalPluginDependencies != null) {
                  Arrays.stream(additionalPluginDependencies)
                      .forEach(additonalPluginDependencyDom -> {
                        String pluginGroupId = getChildParameterValue(additonalPluginDependencyDom, GROUP_ID_ELEMENT, true);
                        String pluginArtifactId =
                            getChildParameterValue(additonalPluginDependencyDom, ARTIFACT_ID_ELEMENT, true);
                        Plugin alreadyDefinedPluginAdditionalDependencies =
                            additionalDependenciesFromMulePlugins.get(pluginGroupId + ":" + pluginArtifactId);
                        List<Dependency> additionalDependencyDependencies = Arrays
                            .stream(additonalPluginDependencyDom.getChild(ADDITIONAL_DEPENDENCIES_ELEMENT)
                                .getChildren(DEPENDENCY_ELEMENT))
                            .map(dependencyDom -> {
                              Dependency dependency = new Dependency();
                              dependency.setGroupId(getChildParameterValue(dependencyDom, GROUP_ID_ELEMENT, true));
                              dependency
                                  .setArtifactId(getChildParameterValue(dependencyDom, ARTIFACT_ID_ELEMENT, true));
                              dependency.setVersion(getChildParameterValue(dependencyDom, VERSION_ELEMENT, true));
                              String type = getChildParameterValue(dependencyDom, "type", false);
                              dependency.setType(type == null ? DEFAULT_ARTIFACT_TYPE : type);
                              dependency.setClassifier(getChildParameterValue(dependencyDom, "classifier", false));
                              dependency.setSystemPath(getChildParameterValue(dependencyDom, "systemPath", false));
                              return dependency;
                            })
                            .collect(toList());
                        if (alreadyDefinedPluginAdditionalDependencies != null) {
                          LinkedList<Dependency> effectiveDependencies =
                              new LinkedList<>(alreadyDefinedPluginAdditionalDependencies.getAdditionalDependencies());
                          additionalDependencyDependencies.forEach(additionalDependenciesDependency -> {
                            boolean addDependency = true;
                            for (int i = 0; i < effectiveDependencies.size(); i++) {
                              Dependency effectiveDependency = effectiveDependencies.get(i);
                              if (effectiveDependency.getGroupId().equals(additionalDependenciesDependency.getGroupId()) &&
                                  effectiveDependency.getArtifactId().equals(additionalDependenciesDependency.getArtifactId())
                                  &&
                                  effectiveDependency.getType().equals(additionalDependenciesDependency.getType()) &&
                                  ObjectUtils.compare(effectiveDependency.getClassifier(),
                                                      additionalDependenciesDependency.getClassifier()) == 0) {
                                if (isNewerVersion(additionalDependenciesDependency.getVersion(),
                                                   effectiveDependency.getVersion())) {
                                  effectiveDependencies.remove(i);
                                } else {
                                  addDependency = false;
                                }
                                break;
                              }
                            }
                            if (addDependency) {
                              effectiveDependencies.add(additionalDependenciesDependency);
                            }
                          });
                          alreadyDefinedPluginAdditionalDependencies.setAdditionalDependencies(effectiveDependencies);
                        } else {
                          Plugin plugin = new Plugin();
                          plugin.setGroupId(pluginGroupId);
                          plugin.setArtifactId(pluginArtifactId);
                          plugin.setAdditionalDependencies(additionalDependencyDependencies);
                          additionalDependenciesFromMulePlugins.put(plugin.getGroupId() + ":" + plugin.getArtifactId(),
                                                                    plugin);
                        }
                      });
                }
              }
            }
          }
        }
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    });
    return additionalDependenciesFromMulePlugins.values();
  }

  private boolean isNewerVersion(String dependencyA, String dependencyB) {
    try {
      return new Semver(dependencyA, LOOSE).isGreaterThan(new Semver(dependencyB, LOOSE));
    } catch (IllegalArgumentException e) {
      // If not using semver lets just compare the strings.
      return dependencyA.compareTo(dependencyB) > 0;
    }
  }

  private Predicate<Plugin> isNotRedefinedAtApplicationLevel() {
    return dependencyPluginAdditionalDependencies -> !pluginsWithAdditionalDependencies.stream()
        .filter(applicationPluginAdditionalDependency -> (dependencyPluginAdditionalDependencies.getGroupId()
            .equals(applicationPluginAdditionalDependency.getGroupId())
            && dependencyPluginAdditionalDependencies.getArtifactId()
                .equals(applicationPluginAdditionalDependency.getArtifactId())))
        .findAny().isPresent();
  }

}
