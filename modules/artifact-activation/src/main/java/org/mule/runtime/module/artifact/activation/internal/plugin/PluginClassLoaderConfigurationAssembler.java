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
import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.MULE_DOMAIN_CLASSIFIER;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.module.artifact.activation.api.ArtifactActivationException;
import org.mule.runtime.module.artifact.activation.internal.classloader.AbstractArtifactClassLoaderConfigurationAssembler;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration.ClassLoaderConfigurationBuilder;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Assembles the class loader configuration for a plugin.
 */
public class PluginClassLoaderConfigurationAssembler extends AbstractArtifactClassLoaderConfigurationAssembler {

  private final File artifactLocation;
  private final List<BundleDependency> bundleDependencies;
  private final BundleDependency bundleDependency;
  private final DeployableArtifactDescriptor ownerDescriptor;
  private final PluginPatchesResolver pluginPatchesResolver;

  public PluginClassLoaderConfigurationAssembler(BundleDependency bundleDependency,
                                                 Set<BundleDescriptor> sharedProjectDependencies,
                                                 File artifactLocation,
                                                 MuleArtifactLoaderDescriptor muleArtifactLoaderDescriptor,
                                                 List<BundleDependency> bundleDependencies,
                                                 DeployableArtifactDescriptor ownerDescriptor) {
    super(bundleDependency.getDescriptor(), muleArtifactLoaderDescriptor);
    this.artifactLocation = artifactLocation;
    this.bundleDependencies = bundleDependencies;
    this.bundleDependency = bundleDependency;
    this.ownerDescriptor = ownerDescriptor;

    Collection<PluginPatchesResolver> resolverRegistered = stream(((Iterable<PluginPatchesResolver>) () -> ServiceLoader
        .load(PluginPatchesResolver.class, this.getClass().getClassLoader()).iterator()).spliterator(), false)
        .collect(toList());
    if (resolverRegistered.size() > 1) {
      throw new MuleRuntimeException(
                                     createStaticMessage("There is more than 1 PluginPatchesResolver implementation registered: "
                                         + resolverRegistered));
    }
    if (resolverRegistered.isEmpty()) {
      pluginPatchesResolver = new NullPluginPatchesResolver();
    } else {
      pluginPatchesResolver = resolverRegistered.iterator().next();
    }
  }

  @Override
  protected List<URL> addArtifactSpecificClassLoaderConfiguration(ClassLoaderConfigurationBuilder classLoaderConfigurationBuilder) {
    // Patches resolution is done just for plugins because this should be the use case, but in the implementation previously used
    // in the Runtime (AbstractMavenClassLoaderConfigurationLoader in versions <= 4.4), it's done for deployables (applications
    // and
    // domains) as well
    pluginPatchesResolver.resolve(bundleDependency.getDescriptor())
        .forEach(classLoaderConfigurationBuilder::containing);

    final List<URL> dependenciesArtifactsUrls = new ArrayList<>();

    if (ownerDescriptor != null) {
      ownerDescriptor.getClassLoaderConfiguration().getDependencies().stream()
          .filter(bundleDescriptor -> bundleDescriptor.getDescriptor().isPlugin())
          .filter(bundleDescriptor -> bundleDescriptor.getDescriptor().getGroupId()
              .equals(bundleDependency.getDescriptor().getGroupId())
              && bundleDescriptor.getDescriptor().getArtifactId().equals(bundleDependency.getDescriptor().getArtifactId()))
          .filter(bundleDependency -> bundleDependency.getAdditionalDependenciesList() != null
              && !bundleDependency.getAdditionalDependenciesList().isEmpty())
          .forEach(bundleDependency -> processPluginAdditionalDependenciesURIs(bundleDependency,
                                                                               classLoaderConfigurationBuilder)
              .forEach(uri -> {
                final URL dependencyArtifactUrl;
                try {
                  dependencyArtifactUrl = uri.toURL();
                } catch (MalformedURLException e) {
                  throw new ArtifactActivationException(createStaticMessage(format("There was an exception obtaining the URL for the artifact [%s], file [%s]",
                                                                                   PluginClassLoaderConfigurationAssembler.this.bundleDependency
                                                                                       .getDescriptor()
                                                                                       .getArtifactFileName(),
                                                                                   uri)),
                                                        e);
                }
                dependenciesArtifactsUrls
                    .add(dependencyArtifactUrl);
              }));
    }

    return dependenciesArtifactsUrls;
  }

  private List<URI> processPluginAdditionalDependenciesURIs(BundleDependency bundleDependency,
                                                            ClassLoaderConfigurationBuilder classLoaderConfigurationBuilder) {
    return bundleDependency.getAdditionalDependenciesList().stream().map(additionalDependency -> {
      if (!isDenylisted(additionalDependency.getDescriptor())) {
        classLoaderConfigurationBuilder.withLocalPackages(additionalDependency.getPackages());
        classLoaderConfigurationBuilder.withLocalResources(additionalDependency.getResources());
      }
      return additionalDependency.getBundleUri();
    }).collect(toList());
  }

  @Override
  protected void populateLocalPackages(ClassLoaderConfigurationBuilder classLoaderConfigurationBuilder) {
    Set<String> packagesSetBuilder = new HashSet<>();
    if (bundleDependency.getPackages() != null) {
      packagesSetBuilder.addAll(bundleDependency.getPackages());
    }

    Set<String> resourcesSetBuilder = new HashSet<>();
    if (bundleDependency.getResources() != null) {
      resourcesSetBuilder.addAll(bundleDependency.getResources());
    }

    bundleDependencies.forEach(dependency -> {
      if (!dependency.getDescriptor().getClassifier().map(MULE_PLUGIN_CLASSIFIER::equals).orElse(false)
          && !dependency.getDescriptor().getClassifier().map(MULE_DOMAIN_CLASSIFIER::equals).orElse(false)
          && !validateMuleRuntimeSharedLibrary(dependency.getDescriptor().getGroupId(),
                                               dependency.getDescriptor().getArtifactId(),
                                               bundleDependency.getDescriptor().getArtifactId())
          && dependency.getBundleUri() != null) {
        if (dependency.getPackages() != null) {
          packagesSetBuilder.addAll(dependency.getPackages());
        }
        if (dependency.getResources() != null) {
          resourcesSetBuilder.addAll(dependency.getResources());
        }
      }
    });

    classLoaderConfigurationBuilder.withLocalPackages(packagesSetBuilder);
    classLoaderConfigurationBuilder.withLocalResources(resourcesSetBuilder);
  }

  @Override
  protected List<BundleDependency> getBundleDependencies() {
    return bundleDependencies;
  }

  @Override
  protected File getProjectFolder() {
    return artifactLocation;
  }

  @Override
  protected ClassLoaderConfigurationBuilder getClassLoaderConfigurationBuilder() {
    return new ClassLoaderConfigurationBuilder();
  }

  @Override
  protected boolean shouldPopulateLocalPackages() {
    return !isDenylisted(bundleDependency.getDescriptor());
  }
}
