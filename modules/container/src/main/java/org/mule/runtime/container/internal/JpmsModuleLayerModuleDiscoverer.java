/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.internal;

import static org.mule.runtime.api.util.MuleSystemProperties.classloaderContainerJpmsModuleLayer;

import static java.lang.ModuleLayer.boot;
import static java.lang.module.ModuleDescriptor.Requires.Modifier.TRANSITIVE;
import static java.util.Collections.emptySet;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

import org.mule.api.annotation.jpms.PrivilegedApi;
import org.mule.runtime.container.api.discoverer.ModuleDiscoverer;
import org.mule.runtime.jpms.api.MuleContainerModule;

import java.io.InputStream;
import java.lang.module.ModuleDescriptor.Exports;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Discovers {@link MuleContainerModule} from java module descriptors.
 *
 * @since 4.6
 */
public class JpmsModuleLayerModuleDiscoverer implements ModuleDiscoverer {

  private final ClasspathModuleDiscoverer fallbackClasspathModuleDiscoverer;

  public JpmsModuleLayerModuleDiscoverer(ClasspathModuleDiscoverer fallbackClasspathModuleDiscoverer) {
    this.fallbackClasspathModuleDiscoverer = fallbackClasspathModuleDiscoverer;
  }

  @Override
  public List<MuleContainerModule> discover() {
    if (!classloaderContainerJpmsModuleLayer()) {
      return fallbackClasspathModuleDiscoverer.discover();
    }

    final List<MuleContainerModule> discoveredModules = getModules(this.getClass().getModule().getLayer())
        .stream()
        .map(jpmsModule -> {
          if (jpmsModule.getDescriptor().isAutomatic()) {
            // fallback
            try (InputStream stream =
                jpmsModule.getResourceAsStream("/" + fallbackClasspathModuleDiscoverer.getModulePropertiesFileName())) {
              final Properties moduleProperties = new Properties();
              if (stream != null) {
                moduleProperties.load(stream);
              } else {
                moduleProperties.put("module.name", jpmsModule.getName());
              }
              return fallbackClasspathModuleDiscoverer.createModule(moduleProperties);
            } catch (Exception e) {
              throw new RuntimeException("Cannot create fallback muleContainer module for '" + jpmsModule.getName() + "'", e);
            }
          } else {
            return new JpmsMuleContainerModule(jpmsModule);
          }
        })
        .collect(toList());

    // In standalone, consider boot modules as well.
    boot()
        .modules()
        .stream()
        .filter(jpmsModule -> jpmsModule.getName().startsWith("org.mule.runtime.")
            || jpmsModule.getName().startsWith("com.mulesoft.mule."))
        .map(JpmsMuleContainerModule::new)
        .forEach(discoveredModules::add);

    return discoveredModules;
  }

  private Set<Module> getModules(ModuleLayer layer) {
    Set<Module> modules = new HashSet<>(layer.modules());
    for (ModuleLayer parent : layer.parents()) {
      modules.addAll(getModules(parent));
    }

    return modules.stream()
        .filter(module -> module.getName().startsWith("org.mule") || module.getName().startsWith("com.mulesoft"))
        .collect(toSet());
  }

  public class JpmsMuleContainerModule implements MuleContainerModule {

    private final Module jpmsModule;

    private final Set<String> exportedPackages;
    private final Set<String> privilegedExportedPackages;
    private final Set<String> privilegedArtifacts;

    public JpmsMuleContainerModule(Module jpmsModule) {
      this.jpmsModule = jpmsModule;

      if (jpmsModule.isAnnotationPresent(PrivilegedApi.class)) {
        final PrivilegedApi privilegedApiAnnotation = jpmsModule.getAnnotation(PrivilegedApi.class);
        this.privilegedExportedPackages = Stream.of(privilegedApiAnnotation.privilegedPackages())
            .collect(toSet());
        this.privilegedArtifacts = Stream.of(privilegedApiAnnotation.privilegedArtifactIds())
            .collect(toSet());
      } else {
        this.privilegedExportedPackages = emptySet();
        this.privilegedArtifacts = emptySet();
      }

      this.exportedPackages = concat(getDirectExports(jpmsModule),
                                     resolveTransitiveExportedPackages(jpmsModule))
          .filter(not(privilegedExportedPackages::contains))
          .collect(toSet());
    }

    private Stream<String> getDirectExports(Module jpmsModule) {
      return jpmsModule.getDescriptor().exports().stream()
          .filter(export -> export.targets().isEmpty())
          .map(Exports::source);
    }

    private Stream<String> resolveTransitiveExportedPackages(Module jpmsModule) {
      return jpmsModule.getDescriptor().requires()
          .stream()
          .filter(required -> required.modifiers().contains(TRANSITIVE))
          .flatMap(required -> {
            final Module requiredTransitiveModule =
                this.getClass().getModule().getLayer().findModule(required.name()).get();

            if (requiredTransitiveModule.getDescriptor().isAutomatic()) {
              return requiredTransitiveModule.getPackages().stream();
            } else {
              return jpmsModule.getLayer().findModule(required.name())
                  .map(requiredModule -> concat(getDirectExports(requiredModule),
                                                resolveTransitiveExportedPackages(requiredModule)))
                  .orElse(Stream.empty());
            }
          });
    }

    @Override
    public String getName() {
      return jpmsModule.getName();
    }

    @Override
    public Set<String> getExportedPackages() {
      return this.exportedPackages;
    }

    @Override
    public Set<String> getExportedPaths() {
      return emptySet();
    }

    @Override
    public Set<String> getPrivilegedExportedPackages() {
      return this.privilegedExportedPackages;
    }

    @Override
    public Set<String> getPrivilegedArtifacts() {
      return this.privilegedArtifacts;
    }

    @Override
    public String toString() {
      return "JpmsMuleContainerModule[" + getName() + "]";
    }
  }

}
