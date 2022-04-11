/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.classloader;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.artifact.api.classloader.ChildOnlyLookupStrategy.CHILD_ONLY;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.DEFAULT_DOMAIN_NAME;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toSet;
import static java.lang.String.format;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.container.internal.ContainerClassLoaderFactory;
import org.mule.runtime.container.internal.ContainerOnlyLookupStrategy;
import org.mule.runtime.module.artifact.activation.api.ArtifactActivationException;
import org.mule.runtime.module.artifact.activation.api.classloader.ArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginDescriptorResolver;
import org.mule.runtime.module.artifact.activation.internal.PluginsDependenciesProcessor;
import org.mule.runtime.module.artifact.activation.internal.nativelib.NativeLibraryFinder;
import org.mule.runtime.module.artifact.activation.internal.nativelib.NativeLibraryFinderFactory;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.api.classloader.ChildFirstLookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.DefaultArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.api.classloader.DelegateOnlyLookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DefaultArtifactClassLoaderResolver implements ArtifactClassLoaderResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultArtifactClassLoaderResolver.class);

  public static final String PLUGIN_CLASSLOADER_IDENTIFIER = "/plugin/";

  private final ModuleRepository moduleRepository;
  private final NativeLibraryFinderFactory nativeLibraryFinderFactory;

  public DefaultArtifactClassLoaderResolver(ModuleRepository moduleRepository,
                                            NativeLibraryFinderFactory nativeLibraryFinderFactory) {
    this.moduleRepository = moduleRepository;
    this.nativeLibraryFinderFactory = nativeLibraryFinderFactory;
  }

  @Override
  public MuleDeployableArtifactClassLoader createDomainClassLoader(DomainDescriptor descriptor) {
    return createDomainClassLoader(descriptor, (ownerClassLoader, artifactPluginDescriptor) -> empty());
  }

  @Override
  public MuleDeployableArtifactClassLoader createDomainClassLoader(DomainDescriptor descriptor,
                                                                   PluginClassLoaderResolver pluginClassLoaderResolver) {
    ArtifactClassLoader parentClassLoader =
        new ContainerClassLoaderFactory(moduleRepository).createContainerClassLoader(this.getClass().getClassLoader());
    String artifactId = getDomainId(descriptor.getName());

    ClassLoaderLookupPolicy parentLookupPolicy = getDomainParentLookupPolicy(parentClassLoader);

    RegionClassLoader regionClassLoader = new RegionClassLoader(artifactId,
                                                                descriptor,
                                                                parentClassLoader.getClassLoader(),
                                                                parentLookupPolicy);

    ArtifactClassLoaderFilter artifactClassLoaderFilter = createArtifactClassLoaderFilter(descriptor, parentLookupPolicy);

    MuleSharedDomainClassLoader domainClassLoader;
    if (descriptor.getName().equals(DEFAULT_DOMAIN_NAME)) {
      domainClassLoader = getDefaultDomainClassLoader(regionClassLoader, regionClassLoader.getClassLoaderLookupPolicy());
    } else {
      NativeLibraryFinder nativeLibraryFinder =
          nativeLibraryFinderFactory.create(descriptor.getDataFolderName(), descriptor.getClassLoaderModel().getUrls());
      domainClassLoader = getCustomDomainClassLoader(regionClassLoader, descriptor, nativeLibraryFinder);
    }

    regionClassLoader.addClassLoader(domainClassLoader, artifactClassLoaderFilter);

    List<ArtifactPluginDescriptor> artifactPluginDescriptors =
        PluginsDependenciesProcessor.process(new ArrayList<>(descriptor.getPlugins()), false, List::add);

    artifactPluginDescriptors
        .stream()
        .map(pluginDependencyDescriptor -> pluginClassLoaderResolver.resolve(domainClassLoader, pluginDependencyDescriptor)
            .orElse(() -> resolvePluginClassLoader(domainClassLoader, pluginDependencyDescriptor)).get())
        .forEach(artifactPluginClassLoader -> regionClassLoader
            .addClassLoader(artifactPluginClassLoader,
                            createPluginClassLoaderFilter(descriptor,
                                                          artifactPluginClassLoader.getArtifactDescriptor(),
                                                          parentLookupPolicy)));

    return domainClassLoader;
  }

  private MuleSharedDomainClassLoader getDefaultDomainClassLoader(ArtifactClassLoader parent,
                                                                  ClassLoaderLookupPolicy containerLookupPolicy) {
    return new MuleSharedDomainClassLoader(new DomainDescriptor(DEFAULT_DOMAIN_NAME), parent.getClassLoader(),
                                           containerLookupPolicy.extend(emptyMap()), emptyList());
  }

  private MuleSharedDomainClassLoader getCustomDomainClassLoader(ArtifactClassLoader parent, DomainDescriptor domain,
                                                                 NativeLibraryFinder nativeLibraryFinder) {
    validateDomain(domain);

    return new MuleSharedDomainClassLoader(domain, parent.getClassLoader(),
                                           getArtifactClassLoaderLookupPolicy(parent, domain),
                                           asList(domain.getClassLoaderModel().getUrls()), nativeLibraryFinder);
  }

  private void validateDomain(DomainDescriptor domainDescriptor) {
    File domainFolder = domainDescriptor.getRootFolder();
    if (!(domainFolder.exists() && domainFolder.isDirectory())) {
      throw new ArtifactActivationException(createStaticMessage(format("Domain %s does not exist", domainDescriptor.getName())));
    }
  }

  /**
   * @param domainName name of the domain. Non empty.
   * @return the unique identifier for the domain in the container.
   */
  public static String getDomainId(String domainName) {
    checkArgument(!isEmpty(domainName), "domainName cannot be empty");

    return "domain/" + domainName;
  }

  private ClassLoaderLookupPolicy getDomainParentLookupPolicy(ArtifactClassLoader parentClassLoader) {
    return parentClassLoader.getClassLoaderLookupPolicy();
  }

  @Override
  public MuleDeployableArtifactClassLoader createApplicationClassLoader(ApplicationDescriptor descriptor,
                                                                        Supplier<ArtifactClassLoader> domainClassLoader) {
    return createApplicationClassLoader(descriptor, domainClassLoader,
                                        (ownerClassLoader, artifactPluginDescriptor) -> empty());
  }

  @Override
  public MuleDeployableArtifactClassLoader createApplicationClassLoader(ApplicationDescriptor descriptor,
                                                                        Supplier<ArtifactClassLoader> domainClassLoader,
                                                                        PluginClassLoaderResolver pluginClassLoaderResolver) {
    ArtifactClassLoader parentClassLoader = domainClassLoader.get();
    String artifactId = getApplicationId(parentClassLoader.getArtifactId(), descriptor.getName());

    ClassLoaderLookupPolicy parentLookupPolicy = getApplicationParentLookupPolicy(parentClassLoader);

    RegionClassLoader regionClassLoader = new RegionClassLoader(artifactId,
                                                                descriptor,
                                                                parentClassLoader.getClassLoader(),
                                                                parentLookupPolicy);

    ArtifactClassLoaderFilter artifactClassLoaderFilter = createArtifactClassLoaderFilter(descriptor, parentLookupPolicy);

    final ClassLoaderLookupPolicy classLoaderLookupPolicy = getArtifactClassLoaderLookupPolicy(parentClassLoader, descriptor);

    MuleDeployableArtifactClassLoader appClassLoader =
        new MuleApplicationClassLoader(artifactId, descriptor, regionClassLoader,
                                       nativeLibraryFinderFactory.create(descriptor.getDataFolderName(),
                                                                         descriptor.getClassLoaderModel().getUrls()),
                                       asList(descriptor.getClassLoaderModel().getUrls()),
                                       classLoaderLookupPolicy);

    regionClassLoader.addClassLoader(appClassLoader, artifactClassLoaderFilter);

    List<ArtifactPluginDescriptor> artifactPluginDescriptors =
        PluginsDependenciesProcessor.process(new ArrayList<>(descriptor.getPlugins()), false, List::add);

    artifactPluginDescriptors
        .stream()
        .map(pluginDependencyDescriptor -> pluginClassLoaderResolver.resolve(appClassLoader, pluginDependencyDescriptor)
            .orElse(() -> resolvePluginClassLoader(appClassLoader, pluginDependencyDescriptor)).get())
        .forEach(artifactPluginClassLoader -> regionClassLoader
            .addClassLoader(artifactPluginClassLoader,
                            createPluginClassLoaderFilter(descriptor,
                                                          artifactPluginClassLoader.getArtifactDescriptor(),
                                                          parentLookupPolicy)));

    return appClassLoader;
  }

  private ClassLoaderLookupPolicy getApplicationParentLookupPolicy(ArtifactClassLoader parentClassLoader) {
    ArtifactDescriptor descriptor = parentClassLoader.getArtifactDescriptor();
    List<String> packages = new ArrayList<>(descriptor.getClassLoaderModel().getExportedPackages());

    if (descriptor instanceof DeployableArtifactDescriptor) {
      for (ArtifactPluginDescriptor artifactPluginDescriptor : ((DeployableArtifactDescriptor) descriptor).getPlugins()) {
        packages.addAll(artifactPluginDescriptor.getClassLoaderModel().getExportedPackages());
      }
    }

    return parentClassLoader.getClassLoaderLookupPolicy().extend(packages.stream(), PARENT_FIRST);
  }

  /**
   * @param domainId      name of the domain where the application is deployed. Non empty.
   * @param applicationId id of the application. Non empty.
   * @return the unique identifier for the application in the container.
   */
  public static String getApplicationId(String domainId, String applicationId) {
    checkArgument(!isEmpty(domainId), "domainId cannot be empty");
    checkArgument(!isEmpty(applicationId), "applicationName cannot be empty");

    return domainId + "/app/" + applicationId;
  }

  //////////////////////////////////////////////////////////
  //
  // mule-application + mule-domain common
  //
  //////////////////////////////////////////////////////////

  private ClassLoaderLookupPolicy getArtifactClassLoaderLookupPolicy(ArtifactClassLoader parent,
                                                                     DeployableArtifactDescriptor descriptor) {
    final List<String> packages = new ArrayList<>();

    for (ArtifactPluginDescriptor artifactPluginDescriptor : descriptor.getPlugins()) {
      packages.addAll(artifactPluginDescriptor.getClassLoaderModel().getExportedPackages());
    }

    return parent.getClassLoaderLookupPolicy().extend(packages.stream(), PARENT_FIRST);
  }

  private ArtifactClassLoaderFilter createArtifactClassLoaderFilter(DeployableArtifactDescriptor artifactDescriptor,
                                                                    ClassLoaderLookupPolicy classLoaderLookupPolicy) {
    Set<String> artifactExportedPackages = sanitizeExportedPackages(artifactDescriptor,
                                                                    classLoaderLookupPolicy,
                                                                    artifactDescriptor.getClassLoaderModel()
                                                                        .getExportedPackages());

    return new DefaultArtifactClassLoaderFilter(artifactExportedPackages,
                                                artifactDescriptor.getClassLoaderModel().getExportedResources());
  }

  private ArtifactClassLoaderFilter createPluginClassLoaderFilter(DeployableArtifactDescriptor artifactDescriptor,
                                                                  ArtifactPluginDescriptor pluginDescriptor,
                                                                  ClassLoaderLookupPolicy classLoaderLookupPolicy) {
    Set<String> sanitizedArtifactExportedPackages =
        sanitizeExportedPackages(artifactDescriptor, classLoaderLookupPolicy,
                                 pluginDescriptor.getClassLoaderModel().getExportedPackages());

    Set<String> replacedPackages =
        artifactDescriptor.getClassLoaderModel().getExportedPackages().stream()
            .filter(p -> sanitizedArtifactExportedPackages.contains(p)).collect(toSet());
    if (!replacedPackages.isEmpty()) {
      sanitizedArtifactExportedPackages.removeAll(replacedPackages);
      LOGGER.warn("Exported packages from plugin '" + pluginDescriptor.getName() + "' are provided by the artifact owner: "
          + replacedPackages);
    }
    return new DefaultArtifactClassLoaderFilter(sanitizedArtifactExportedPackages,
                                                pluginDescriptor.getClassLoaderModel().getExportedResources());
  }

  private Set<String> sanitizeExportedPackages(DeployableArtifactDescriptor artifactDescriptor,
                                               ClassLoaderLookupPolicy classLoaderLookupPolicy,
                                               Set<String> artifactExportedPackages) {
    Set<String> sanitizedArtifactExportedPackages = new HashSet<>(artifactExportedPackages);

    Set<String> containerProvidedPackages = sanitizedArtifactExportedPackages.stream().filter(p -> {
      LookupStrategy lookupStrategy = classLoaderLookupPolicy.getPackageLookupStrategy(p);
      return !(lookupStrategy instanceof ChildFirstLookupStrategy);
    }).collect(toSet());
    if (!containerProvidedPackages.isEmpty()) {
      sanitizedArtifactExportedPackages.removeAll(containerProvidedPackages);
      LOGGER.warn("Exported packages from artifact '" + artifactDescriptor.getName() + "' are provided by parent class loader: "
          + containerProvidedPackages);
    }
    return sanitizedArtifactExportedPackages;
  }

  //////////////////////////////////////////////////////////
  //
  // mule-plugin
  //
  //////////////////////////////////////////////////////////

  @Override
  public MuleArtifactClassLoader createMulePluginClassLoader(MuleDeployableArtifactClassLoader ownerArtifactClassLoader,
                                                             ArtifactPluginDescriptor descriptor,
                                                             PluginDescriptorResolver pluginDescriptorResolver) {
    return createMulePluginClassLoader(ownerArtifactClassLoader, descriptor, pluginDescriptorResolver,
                                       (ownerClassLoader, artifactPluginDescriptor) -> empty());

  }

  @Override
  public MuleArtifactClassLoader createMulePluginClassLoader(MuleDeployableArtifactClassLoader ownerArtifactClassLoader,
                                                             ArtifactPluginDescriptor descriptor,
                                                             PluginDescriptorResolver pluginDescriptorResolver,
                                                             PluginClassLoaderResolver pluginClassLoaderResolver) {
    RegionClassLoader regionClassLoader = (RegionClassLoader) ownerArtifactClassLoader.getParent();
    final String pluginArtifactId = getArtifactPluginId(regionClassLoader.getArtifactId(), descriptor.getName());

    ClassLoaderLookupPolicy pluginLookupPolicy = createPluginLookupPolicy(descriptor,
                                                                          regionClassLoader,
                                                                          pluginDescriptorResolver,
                                                                          pluginClassLoaderResolver);

    MuleArtifactClassLoader pluginClassLoader =
        new MuleArtifactClassLoader(pluginArtifactId, descriptor, descriptor.getClassLoaderModel().getUrls(),
                                    regionClassLoader, pluginLookupPolicy);
    return pluginClassLoader;
  }

  protected ClassLoaderLookupPolicy createPluginLookupPolicy(ArtifactPluginDescriptor descriptor,
                                                             RegionClassLoader regionClassLoader,
                                                             PluginDescriptorResolver pluginDescriptorResolver,
                                                             PluginClassLoaderResolver pluginClassLoaderResolver) {
    ClassLoaderLookupPolicy baseLookupPolicy = regionClassLoader.getClassLoaderLookupPolicy()
        .extend(regionClassLoader.filterForClassLoader(regionClassLoader.getOwnerClassLoader())
            .getExportedClassPackages()
            .stream(), PARENT_FIRST);

    Map<String, LookupStrategy> pluginsLookupPolicies = new HashMap<>();

    descriptor.getClassLoaderModel().getDependencies()
        .stream()
        .filter(dependency -> dependency.getDescriptor().getClassifier()
            .map(MULE_PLUGIN_CLASSIFIER::equals)
            .orElse(false))
        .map(dependency -> pluginDescriptorResolver.resolve(dependency.getDescriptor()))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .forEach(dependencyPluginDescriptor -> {
          for (String exportedPackage : dependencyPluginDescriptor.getClassLoaderModel().getExportedPackages()) {
            pluginsLookupPolicies.put(exportedPackage, PARENT_FIRST);
          }

          if (isPrivilegedPluginDependency(descriptor, dependencyPluginDescriptor)) {
            ArtifactClassLoader pluginClassLoader = pluginClassLoaderResolver
                .resolve(regionClassLoader.getOwnerClassLoader(), dependencyPluginDescriptor)
                .orElse(() -> regionClassLoader.getArtifactPluginClassLoaders()
                    .stream().filter(
                                     c -> c
                                         .getArtifactDescriptor()
                                         .getBundleDescriptor()
                                         .getArtifactId()
                                         .equals(dependencyPluginDescriptor
                                             .getBundleDescriptor()
                                             .getArtifactId()))
                    .findAny()
                    .orElseThrow(() -> new ArtifactActivationException(createStaticMessage("Cannot find classloader for plugin: "
                        + dependencyPluginDescriptor.getBundleDescriptor().getArtifactId()))))
                .get();
            LookupStrategy lookupStrategy = new DelegateOnlyLookupStrategy(pluginClassLoader.getClassLoader());

            for (String exportedPackage : dependencyPluginDescriptor.getClassLoaderModel().getPrivilegedExportedPackages()) {
              pluginsLookupPolicies.put(exportedPackage, lookupStrategy);
            }
          }
        });

    ContainerOnlyLookupStrategy containerOnlyLookupStrategy = new ContainerOnlyLookupStrategy(this.getClass().getClassLoader());
    Set<String> muleModulesExportedPackages = new HashSet<>();

    for (MuleModule module : moduleRepository.getModules()) {
      if (module.getPrivilegedArtifacts()
          .contains(descriptor.getBundleDescriptor().getGroupId() + ":" + descriptor.getBundleDescriptor().getArtifactId())) {
        for (String packageName : module.getPrivilegedExportedPackages()) {
          pluginsLookupPolicies.put(packageName, containerOnlyLookupStrategy);
        }
      }

      muleModulesExportedPackages.addAll(module.getExportedPackages());
    }

    List<String> pluginLocalPackages = new ArrayList<>();
    for (String localPackage : descriptor.getClassLoaderModel().getLocalPackages()) {
      // packages exported from another artifact in the region will be ParentFirst,
      // even if they are also exported by the container.
      if (baseLookupPolicy.getPackageLookupStrategy(localPackage) instanceof ContainerOnlyLookupStrategy
          || (baseLookupPolicy.getPackageLookupStrategy(localPackage) instanceof ParentFirstLookupStrategy
              && muleModulesExportedPackages.contains(localPackage))) {
        LOGGER.debug("Plugin '" + descriptor.getName() + "' contains a local package '" + localPackage
            + "', but it will be ignored since it is already available from the container.");
      } else {
        pluginLocalPackages.add(localPackage);
      }
    }

    return baseLookupPolicy.extend(pluginsLookupPolicies).extend(pluginLocalPackages.stream(), CHILD_ONLY, true);
  }

  /**
   * @param parentArtifactId identifier of the artifact that owns the plugin. Non empty.
   * @param pluginName       name of the plugin. Non empty.
   * @return the unique identifier for the plugin inside the parent artifact.
   */
  private String getArtifactPluginId(String parentArtifactId, String pluginName) {
    checkArgument(!isEmpty(parentArtifactId), "parentArtifactId cannot be empty");
    checkArgument(!isEmpty(pluginName), "pluginName cannot be empty");

    return parentArtifactId + PLUGIN_CLASSLOADER_IDENTIFIER + pluginName;
  }

  private boolean isPrivilegedPluginDependency(ArtifactPluginDescriptor descriptor,
                                               ArtifactPluginDescriptor dependencyPluginDescriptor) {
    if (dependencyPluginDescriptor.getClassLoaderModel().getPrivilegedExportedPackages().isEmpty()) {
      return false;
    }

    return dependencyPluginDescriptor.getClassLoaderModel().getPrivilegedArtifacts()
        .stream()
        .filter(a -> a.startsWith(descriptor.getBundleDescriptor().getGroupId()
            + ":" + descriptor.getBundleDescriptor().getArtifactId()))
        .findFirst().isPresent();
  }

  private MuleArtifactClassLoader resolvePluginClassLoader(ArtifactClassLoader ownerClassLoader,
                                                           ArtifactPluginDescriptor descriptor) {
    return createMulePluginClassLoader((MuleDeployableArtifactClassLoader) ownerClassLoader,
                                       descriptor,
                                       bundleDescriptor -> ((DeployableArtifactDescriptor) ownerClassLoader
                                           .getArtifactDescriptor())
                                               .getPlugins()
                                               .stream()
                                               .filter(apd -> apd.getBundleDescriptor().getArtifactId()
                                                   .equals(bundleDescriptor.getArtifactId())
                                                   && apd.getBundleDescriptor().getGroupId()
                                                       .equals(bundleDescriptor.getGroupId()))
                                               .findAny());
  }

}
