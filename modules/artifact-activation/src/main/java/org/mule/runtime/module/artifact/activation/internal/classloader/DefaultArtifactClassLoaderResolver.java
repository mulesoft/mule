/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.classloader;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.artifact.api.classloader.ChildOnlyLookupStrategy.CHILD_ONLY;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.DEFAULT_DOMAIN_NAME;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toSet;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.mule.runtime.api.util.collection.SmallMap;
import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.container.internal.ContainerClassLoaderFactory;
import org.mule.runtime.container.internal.ContainerOnlyLookupStrategy;
import org.mule.runtime.module.artifact.activation.api.classloader.ArtifactClassLoaderResolver;
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
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DefaultArtifactClassLoaderResolver implements ArtifactClassLoaderResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultArtifactClassLoaderResolver.class);

  public static final String PLUGIN_CLASSLOADER_IDENTIFIER = "/plugin/";

  private final ModuleRepository moduleRepository;
  // TODO what about native libraries?
  private final NativeLibraryFinderFactory nativeLibraryFinderFactory;

  // TODO provide a default constructor as well
  public DefaultArtifactClassLoaderResolver(ModuleRepository moduleRepository,
                                            NativeLibraryFinderFactory nativeLibraryFinderFactory) {
    this.moduleRepository = moduleRepository;
    this.nativeLibraryFinderFactory = nativeLibraryFinderFactory;
  }

  @Override
  public MuleDeployableArtifactClassLoader createDomainClassLoader(DomainDescriptor descriptor,
                                                                   BiFunction<ArtifactClassLoader, ArtifactPluginDescriptor, ArtifactClassLoader> pluginClassLoaderResolver) {
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

    descriptor.getPlugins()
        .stream()
        .map(pluginDependencyDescriptor -> pluginClassLoaderResolver.apply(domainClassLoader, pluginDependencyDescriptor))
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
                                           containerLookupPolicy.extend(emptyMap()), emptyList(), emptyList());
  }

  private MuleSharedDomainClassLoader getCustomDomainClassLoader(ArtifactClassLoader parent, DomainDescriptor domain,
                                                                 NativeLibraryFinder nativeLibraryFinder) {
    // validateDomain(domain);

    return new MuleSharedDomainClassLoader(domain, parent.getClassLoader(),
                                           getArtifactClassLoaderLookupPolicy(parent, domain),
                                           asList(domain.getClassLoaderModel().getUrls()),
                                           emptyList(), nativeLibraryFinder);
  }

  /**
   * @param domainName name of the domain. Non empty.
   * @return the unique identifier for the domain in the container.
   */
  private String getDomainId(String domainName) {
    checkArgument(!isEmpty(domainName), "domainName cannot be empty");

    return "/domain/" + domainName;
  }

  private ClassLoaderLookupPolicy getDomainParentLookupPolicy(ArtifactClassLoader parentClassLoader) {
    return parentClassLoader.getClassLoaderLookupPolicy();
  }

  @Override
  public MuleDeployableArtifactClassLoader createApplicationClassLoader(ApplicationDescriptor descriptor,
                                                                        Function<Optional<BundleDescriptor>, MuleDeployableArtifactClassLoader> domainClassLoaderResolver,
                                                                        BiFunction<ArtifactClassLoader, ArtifactPluginDescriptor, ArtifactClassLoader> pluginClassLoaderResolver) {



    ArtifactClassLoader parentClassLoader = domainClassLoaderResolver.apply(descriptor.getDomainDescriptor());
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
                                       classLoaderLookupPolicy,
                                       emptyList());

    regionClassLoader.addClassLoader(appClassLoader, artifactClassLoaderFilter);

    descriptor.getPlugins()
        .stream()
        .map(pluginDependencyDescriptor -> pluginClassLoaderResolver.apply(appClassLoader, pluginDependencyDescriptor))
        .forEach(artifactPluginClassLoader -> regionClassLoader
            .addClassLoader(artifactPluginClassLoader,
                            createPluginClassLoaderFilter(descriptor,
                                                          artifactPluginClassLoader.getArtifactDescriptor(),
                                                          parentLookupPolicy)));

    return appClassLoader;
  }

  private ClassLoaderLookupPolicy getApplicationParentLookupPolicy(ArtifactClassLoader parentClassLoader) {
    Map<String, LookupStrategy> lookupStrategies = new SmallMap<>();

    ArtifactDescriptor descriptor = parentClassLoader.getArtifactDescriptor();
    descriptor.getClassLoaderModel().getExportedPackages().forEach(p -> lookupStrategies.put(p, PARENT_FIRST));

    if (descriptor instanceof DeployableArtifactDescriptor) {
      for (ArtifactPluginDescriptor artifactPluginDescriptor : ((DeployableArtifactDescriptor) descriptor).getPlugins()) {
        artifactPluginDescriptor.getClassLoaderModel().getExportedPackages()
            .forEach(p -> lookupStrategies.put(p, PARENT_FIRST));
      }
    }

    return parentClassLoader.getClassLoaderLookupPolicy().extend(lookupStrategies);
  }

  /**
   * @param domainId      name of the domain where the application is deployed. Non empty.
   * @param applicationId id of the application. Non empty.
   * @return the unique identifier for the application in the container.
   */
  public String getApplicationId(String domainId, String applicationId) {
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

    final Map<String, LookupStrategy> pluginsLookupStrategies = new HashMap<>();

    for (ArtifactPluginDescriptor artifactPluginDescriptor : descriptor.getPlugins()) {
      artifactPluginDescriptor.getClassLoaderModel().getExportedPackages()
          .forEach(p -> pluginsLookupStrategies.put(p, PARENT_FIRST));
    }

    return parent.getClassLoaderLookupPolicy().extend(pluginsLookupStrategies);
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
                                                             Function<BundleDescriptor, Optional<ArtifactPluginDescriptor>> pluginDescriptorResolver,
                                                             BiFunction<ArtifactClassLoader, ArtifactPluginDescriptor, ArtifactClassLoader> pluginClassLoaderResolver) {
    RegionClassLoader regionClassLoader = (RegionClassLoader) ownerArtifactClassLoader.getParent();
    final String pluginArtifactId = getArtifactPluginId(regionClassLoader.getArtifactId(), descriptor.getName());

    ClassLoaderLookupPolicy pluginLookupPolicy = createPluginLookupPolicy(descriptor,
                                                                          regionClassLoader,
                                                                          pluginDescriptorResolver, pluginClassLoaderResolver);

    MuleArtifactClassLoader pluginClassLoader =
        new MuleArtifactClassLoader(pluginArtifactId, descriptor, descriptor.getClassLoaderModel().getUrls(),
                                    regionClassLoader, pluginLookupPolicy);
    return pluginClassLoader;
  }

  protected ClassLoaderLookupPolicy createPluginLookupPolicy(ArtifactPluginDescriptor descriptor,
                                                             RegionClassLoader regionClassLoader,
                                                             Function<BundleDescriptor, Optional<ArtifactPluginDescriptor>> pluginDescriptorResolver,
                                                             BiFunction<ArtifactClassLoader, ArtifactPluginDescriptor, ArtifactClassLoader> pluginClassLoaderResolver) {
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
        .map(pluginDescriptorResolver.compose(BundleDependency::getDescriptor))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .forEach(dependencyPluginDescriptor -> {
          for (String exportedPackage : dependencyPluginDescriptor.getClassLoaderModel().getExportedPackages()) {
            pluginsLookupPolicies.put(exportedPackage, PARENT_FIRST);
          }

          if (isPrivilegedPluginDependency(descriptor, dependencyPluginDescriptor)) {
            ArtifactClassLoader pluginClassLoader =
                pluginClassLoaderResolver.apply(regionClassLoader.getOwnerClassLoader(), dependencyPluginDescriptor);
            if (pluginClassLoader == null) {
              throw new IllegalStateException("Cannot find classloader for plugin: "
                  + dependencyPluginDescriptor.getBundleDescriptor().getArtifactId());
            }
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

    Map<String, LookupStrategy> pluginLocalPolicies = new HashMap<>();
    for (String localPackage : descriptor.getClassLoaderModel().getLocalPackages()) {
      // packages exported from another artifact in the region will be ParentFirst,
      // even if they are also exported by the container.
      if (baseLookupPolicy.getPackageLookupStrategy(localPackage) instanceof ContainerOnlyLookupStrategy
          || (baseLookupPolicy.getPackageLookupStrategy(localPackage) instanceof ParentFirstLookupStrategy
              && muleModulesExportedPackages.contains(localPackage))) {
        LOGGER.debug("Plugin '" + descriptor.getName() + "' contains a local package '" + localPackage
            + "', but it will be ignored since it is already available from the container.");
      } else {
        pluginLocalPolicies.put(localPackage, CHILD_ONLY);
      }
    }

    return baseLookupPolicy.extend(pluginsLookupPolicies).extend(pluginLocalPolicies, true);
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


  @Override
  public MuleArtifactClassLoader resolvePluginClassLoader(ArtifactClassLoader ownerClassLoader,
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
                                               .findAny(),
                                       this::resolvePluginClassLoader);
  }

}
