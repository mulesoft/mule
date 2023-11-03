/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.jpms.api;

import static java.lang.Boolean.getBoolean;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.lang.ModuleLayer.boot;
import static java.lang.ModuleLayer.defineModulesWithOneLoader;
import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;
import static java.lang.System.getProperty;
import static java.lang.module.Configuration.resolve;
import static java.lang.module.ModuleFinder.ofSystem;
import static java.nio.file.Paths.get;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.lang.ModuleLayer.Controller;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Utilities related to how Mule uses the Java Module system.
 * 
 * @since 4.5
 */
public final class JpmsUtils {

  private JpmsUtils() {
    // Nothing to do
  }

  private static final int JAVA_MAJOR_VERSION = parseInt(getProperty("java.version").split("\\.")[0]);
  // this is copied from MuleSystemProperties so we don't have to add the mule-api dependency on the bootstrap.
  private static final String CLASSLOADER_CONTAINER_JPMS_MODULE_LAYER = "mule.classloader.container.jpmsModuleLayer";

  public static final String MULE_SKIP_MODULE_TWEAKING_VALIDATION = "mule.module.tweaking.validation.skip";

  private static final Set<String> SERVICE_MODULE_NAME_PREFIXES =
      new HashSet<>(asList("org.mule.service.",
                           "com.mulesoft.mule.service.",
                           "com.mulesoft.anypoint.gw.service."));

  private static final String REQUIRED_ADD_MODULES =
      "--add-modules="
          + "java.se,"
          + "org.mule.boot.tanuki,"
          + "org.mule.runtime.jpms.utils,"
          + "com.fasterxml.jackson.core";
  private static final String REQUIRED_ADD_OPENS_JAVA_LANG =
      "--add-opens=java.base/java.lang=org.mule.runtime.jpms.utils";
  private static final String REQUIRED_ADD_OPENS_JAVA_LANG_REFLECT =
      "--add-opens=java.base/java.lang.reflect=org.mule.runtime.jpms.utils";
  private static final String REQUIRED_ADD_OPENS_JAVA_SECURITY_CERT =
      "--add-opens=java.base/java.security.cert=org.mule.runtime.jpms.utils";

  /**
   * Validates that no module tweaking jvm options (i.e: {@code --add-opens}, {@code --add-exports}, ...) have been provided in
   * addition to the minimal required by the Mule Runtime to function properly.
   */
  public static void validateNoBootModuleLayerTweaking() {
    if (getBoolean(MULE_SKIP_MODULE_TWEAKING_VALIDATION)) {
      System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
      System.err.println("!! WARNING!");
      System.err.println("!! '" + MULE_SKIP_MODULE_TWEAKING_VALIDATION
          + "' property MUST ONLY be used temporarily in development environments.");
      System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

      return;
    }

    RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
    List<String> arguments = runtimeMxBean.getInputArguments();

    List<String> illegalArguments = arguments
        .stream()
        .filter(arg -> arg.startsWith("--add-exports")
            || arg.startsWith("--add-opens")
            || arg.startsWith("--add-modules")
            || arg.startsWith("--add-reads")
            || arg.startsWith("--patch-module"))
        .filter(arg -> !(arg.equals(REQUIRED_ADD_MODULES)
            || arg.equals(REQUIRED_ADD_OPENS_JAVA_LANG)
            || arg.equals(REQUIRED_ADD_OPENS_JAVA_LANG_REFLECT)
            || arg.equals(REQUIRED_ADD_OPENS_JAVA_SECURITY_CERT)))
        .collect(toList());

    if (!illegalArguments.isEmpty()) {
      throw new IllegalArgumentException("Invalid module tweaking options passed to the JVM running the Mule Runtime: "
          + illegalArguments);
    }
  }

  /**
   * 
   * Search for packages using the JRE's module architecture. (Java 9 and above).
   *
   * @param packages where to add new found packages
   */
  public static void exploreJdkModules(Set<String> packages) {
    boot().modules()
        .stream()
        .filter(module -> {
          final String moduleName = module.getName();

          // Original intention is to only expose standard java modules...
          return moduleName.startsWith("java.")
              // ... but because we need to keep compatibility with Java 8, older versions of some libraries use internal JDK
              // modules packages:
              // * caffeine 2.x still uses sun.misc.Unsafe. Ref: https://github.com/ben-manes/caffeine/issues/273
              // * obgenesis SunReflectionFactoryHelper, used by Mockito
              || moduleName.startsWith("jdk.");
        })
        .forEach(module -> packages.addAll(module.getPackages()));
  }

  /**
   * Creates a {@link ModuleLayer} for the given {@code modulePathEntries} and with the given {@code parent}, and returns a
   * classLoader from which its modules can be read.
   * 
   * @param modulePathEntries the URLs from which to find the modules
   * @param parent            the parent class loader for delegation
   * @return a new classLoader.
   */
  public static ClassLoader createModuleLayerClassLoader(URL[] modulePathEntries, ClassLoader parent) {
    if (!useModuleLayer()) {
      return new URLClassLoader(modulePathEntries, parent);
    }

    final ModuleLayer layer = createModuleLayer(modulePathEntries, parent, empty(), false, true);
    return layer.findLoader(layer.modules().iterator().next().getName());
  }

  /**
   * Creates two {@link ModuleLayer}s for the given {@code modulePathEntriesParent} and {@code modulePathEntriesChild} and with
   * the given {@code parent}, and returns a classLoader from which the child modules can be read.
   * 
   * @param modulePathEntriesParent the URLs from which to find the modules of the parent
   * @param modulePathEntriesChild  the URLs from which to find the modules of the child
   * @param childClassLoaderFactory how the classLoader for the child is created, if moduleLayers are not used
   * @param parent                  the parent class loader for delegation
   * @return a new classLoader.
   */
  public static ClassLoader createModuleLayerClassLoader(URL[] modulePathEntriesParent, URL[] modulePathEntriesChild,
                                                         MultiLevelClassLoaderFactory childClassLoaderFactory,
                                                         ClassLoader parent) {
    return createModuleLayerClassLoader(modulePathEntriesParent, modulePathEntriesChild, childClassLoaderFactory, parent,
                                        empty());
  }

  /**
   * Creates two classLoaders for the given {@code modulePathEntriesParent} and {@code modulePathEntriesChild}, with the layer
   * from the given {@code clazz} as parent, if any, and with the given {@code parentClassLoader}. A classLoader from which the
   * child modules can be read is returned.
   *
   * @param modulePathEntriesParent the URLs from which to find the modules of the parent
   * @param modulePathEntriesChild  the URLs from which to find the modules of the child
   * @param childClassLoaderFactory how the classLoader for the child is created, if moduleLayers are not used
   * @param parentClassLoader       the parent class loader for delegation
   * @param clazz                   the class from which to get the parent layer.
   * @return a new classLoader.
   */
  public static ClassLoader createModuleLayerClassLoader(URL[] modulePathEntriesParent, URL[] modulePathEntriesChild,
                                                         MultiLevelClassLoaderFactory childClassLoaderFactory,
                                                         ClassLoader parentClassLoader,
                                                         Optional<Class> clazz) {
    if (!useModuleLayer()) {
      return childClassLoaderFactory.create(parentClassLoader, modulePathEntriesParent, modulePathEntriesChild);
    }

    ModuleLayer resolvedParentLayer = clazz.map(cl -> cl.getModule().getLayer()).orElse(null);
    final ModuleLayer parentLayer =
        createModuleLayer(modulePathEntriesParent, parentClassLoader, ofNullable(resolvedParentLayer), false, true);
    ClassLoader childParentClassLoader = parentLayer.findLoader(parentLayer.modules().iterator().next().getName());
    final ModuleLayer childLayer =
        createModuleLayer(modulePathEntriesChild, childParentClassLoader, of(parentLayer), false, true);
    openToModule(childLayer, "org.mule.runtime.launcher", "org.mule.boot.api",
                 asList("org.mule.runtime.module.boot.internal"));
    openToModule(childLayer, "kryo.shaded", "java.base",
                 asList("java.lang", "java.lang.reflect"));

    return childLayer.findLoader(childLayer.modules().iterator().next().getName());
  }

  private static boolean useModuleLayer() {
    return parseBoolean(getProperty(CLASSLOADER_CONTAINER_JPMS_MODULE_LAYER, "" + (JAVA_MAJOR_VERSION >= 17)));
  }

  /**
   * Creates a {@link ModuleLayer} for the given {@code modulePathEntries} and with the given {@code parent}.
   * <p>
   * Note: By definition, automatic modules have transitive readability on ALL other modules on the same layer and the parents.
   * This may cause a situation where a layer that is supposed to be isolated will instead be able to read all the modules in the
   * parent layers. To prevent this, the {@code isolateDependenciesInTheirOwnLayer} parameter must be passed as {@code true}.
   *
   * @param modulePathEntries                  the URLs from which to find the modules
   * @param parent                             the parent class loader for delegation
   * @param parentLayer                        a layer of modules that will be visible from the newly created {@link ModuleLayer}.
   * @param isolateDependenciesInTheirOwnLayer whether an additional {@link ModuleLayer} having only the {@code boot} layer as
   *                                           parent will be created for modules that need to be isolated.
   * @return a new {@link ModuleLayer}.
   */
  public static ModuleLayer createModuleLayer(URL[] modulePathEntries, ClassLoader parent, Optional<ModuleLayer> parentLayer,
                                              boolean isolateDependenciesInTheirOwnLayer,
                                              boolean filterBootModules) {
    final Set<String> bootModules;
    if (filterBootModules) {
      bootModules = boot().modules().stream()
          .map(m -> m.getName())
          .collect(toSet());
    } else {
      bootModules = emptySet();
    }

    Path[] paths = Stream.of(modulePathEntries)
        .map(url -> {
          try {
            return get(url.toURI());
          } catch (URISyntaxException e) {
            throw new RuntimeException(e);
          }
        })
        .toArray(size -> new Path[size]);

    ModuleFinder modulesFinder = ModuleFinder.of(paths);

    Map<Boolean, List<ModuleReference>> modulesByIsolation = modulesFinder
        .findAll()
        .stream()
        .filter(moduleRef -> !bootModules.contains(moduleRef.descriptor().name()))
        .collect(partitioningBy(moduleRef -> isolateInOrphanLayer(moduleRef, parentLayer)));

    ModuleLayer resolvedParentLayer = parentLayer.orElse(boot());

    Controller controller;
    if (isolateDependenciesInTheirOwnLayer) {
      // put all modules requiring isolation in their own layer, having only boot layer as parent...
      Path[] isolatedModulesPaths = modulesByIsolation.get(true)
          .stream()
          .map(ModuleReference::location)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .map(uri -> get(uri))
          .toArray(size -> new Path[size]);
      ModuleFinder isolatedModulesFinder = ModuleFinder.of(isolatedModulesPaths);
      Configuration isolatedModulesConfiguration = boot().configuration()
          .resolve(isolatedModulesFinder, ofSystem(), modulesByIsolation.get(true)
              .stream()
              .map(moduleRef -> moduleRef.descriptor().name())
              .collect(toList()));
      Controller isolatedModulesController = defineModulesWithOneLoader(isolatedModulesConfiguration,
                                                                        singletonList(boot()),
                                                                        parent);

      // ... the put the rest of the modules on a new layer with the isolated modules one as parent.
      Path[] notIsolatedModulesPaths = modulesByIsolation.get(false)
          .stream()
          .map(ModuleReference::location)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .map(uri -> get(uri))
          .toArray(size -> new Path[size]);
      ModuleFinder notIsolatedModulesFinder = ModuleFinder.of(notIsolatedModulesPaths);
      Configuration configuration = resolve(notIsolatedModulesFinder,
                                            asList(isolatedModulesController.layer().configuration(),
                                                   resolvedParentLayer.configuration()),
                                            ofSystem(),
                                            modulesByIsolation.get(false).stream()
                                                .map(moduleRef -> moduleRef.descriptor().name())
                                                .collect(toList()));
      controller = defineModulesWithOneLoader(configuration,
                                              asList(isolatedModulesController.layer(), resolvedParentLayer),
                                              parent);

    } else {
      Path[] filteredModulesPaths = modulesByIsolation.values().stream()
          .flatMap(Collection::stream)
          .map(ModuleReference::location)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .map(uri -> get(uri))
          .toArray(size -> new Path[size]);
      ModuleFinder filteredModulesFinder = ModuleFinder.of(filteredModulesPaths);

      Configuration configuration = resolvedParentLayer.configuration()
          .resolve(filteredModulesFinder, ofSystem(), modulesByIsolation.values().stream()
              .flatMap(Collection::stream)
              .map(modueRef -> modueRef.descriptor().name())
              .collect(toList()));
      controller = defineModulesWithOneLoader(configuration,
                                              singletonList(resolvedParentLayer),
                                              parent);
    }


    return controller.layer();
  }

  /**
   *
   * @param moduleRef
   * @param containerLayer
   * @return {@code true} the the referenced module is automatic or does not read any module from the container.
   */
  private static boolean isolateInOrphanLayer(ModuleReference moduleRef, Optional<ModuleLayer> containerLayer) {
    if (moduleRef.descriptor().isAutomatic()
        // TODO W-13761983 remove this condition
        && SERVICE_MODULE_NAME_PREFIXES.stream()
            .noneMatch(moduleRef.descriptor().name()::startsWith)) {
      return true;
    }

    final Set<String> containerModuleNames = containerLayer
        .map(layer -> layer.modules().stream()
            .map(Module::getName)
            .collect(toSet()))
        .orElse(emptySet());

    return containerLayer
        .map(layer -> moduleRef.descriptor().requires().stream()
            .noneMatch(req -> containerModuleNames.contains(req.name())))
        .orElse(false);
  }

  /**
   *
   * @param layer          the layer containing the module to open the {@code packages} to.
   * @param moduleName     the name of the module within {@code layer} to open the {@code packages} to.
   * @param bootModuleName the name of the module in the boot layer to open the {@code packages} from.
   * @param packages       the packages to open from {@code bootModuleName} to {@code moduleName}.
   */
  public static void openToModule(ModuleLayer layer, String moduleName, String bootModuleName, List<String> packages) {
    // Make sure only allowed users within the Mule Runtime use this
    final String callerClassName = StackWalker.getInstance(RETAIN_CLASS_REFERENCE).getCallerClass().getName();
    if (!(callerClassName.equals("org.mule.runtime.module.service.api.artifact.ServiceModuleLayerFactory")
        || callerClassName.equals("org.mule.runtime.jpms.api.JpmsUtils"))) {
      throw new UnsupportedOperationException("This is for internal use only.");
    }

    layer.findModule(moduleName)
        .ifPresent(module -> boot().findModule(bootModuleName)
            .ifPresent(bootModule -> {
              for (String pkg : packages) {
                bootModule.addOpens(pkg, module);
              }
            }));
  }

}
