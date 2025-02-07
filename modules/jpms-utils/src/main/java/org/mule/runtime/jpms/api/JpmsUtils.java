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
import static java.lang.management.ManagementFactory.getRuntimeMXBean;
import static java.lang.module.Configuration.resolve;
import static java.lang.module.ModuleFinder.ofSystem;
import static java.nio.file.Paths.get;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.lang.ModuleLayer.Controller;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

  private static final String MULE_JPMS_UTILS_MODULE_NAME = "org.mule.runtime.jpms.utils";
  private static final String MULE_LAUNCHER_MODULE_NAME = "org.mule.runtime.launcher";
  private static final String HAZELCAST_CORE_MODULE_NAME = "com.hazelcast.core";
  private static final String KRYO_MODULE_NAME = "kryo.shaded";

  private static final Set<String> SERVICE_MODULE_NAME_PREFIXES =
      new HashSet<>(asList("org.mule.service.",
                           "com.mulesoft.mule.service.",
                           "com.mulesoft.anypoint.gw.service."));

  private static final Set<String> REQUIRED_ADD_MODULES =
      new HashSet<>(asList("java.se",
                           "org.mule.boot.tanuki",
                           "org.mule.runtime.boot.log4j",
                           "org.mule.runtime.logging",
                           MULE_JPMS_UTILS_MODULE_NAME,
                           "com.fasterxml.jackson.core",
                           "org.apache.commons.codec"));
  private static final String REQUIRED_ADD_OPENS_JAVA_LANG =
      "--add-opens=java.base/java.lang=org.mule.runtime.jpms.utils";
  private static final String REQUIRED_ADD_OPENS_JAVA_LANG_REFLECT =
      "--add-opens=java.base/java.lang.reflect=org.mule.runtime.jpms.utils";
  private static final String REQUIRED_ADD_OPENS_JAVA_LANG_INVOKE =
      "--add-opens=java.base/java.lang.invoke=org.mule.runtime.jpms.utils";
  private static final String REQUIRED_ADD_OPENS_JDK_INTERNAL_REF =
      "--add-opens=java.base/jdk.internal.ref=org.mule.runtime.jpms.utils";
  private static final String REQUIRED_ADD_OPENS_JAVA_NIO =
      "--add-opens=java.base/java.nio=org.mule.runtime.jpms.utils";
  private static final String REQUIRED_ADD_OPENS_SUN_NIO_CH =
      "--add-opens=java.base/sun.nio.ch=org.mule.runtime.jpms.utils";
  private static final String REQUIRED_ADD_OPENS_JAVA_SQL =
      "--add-opens=java.sql/java.sql=org.mule.runtime.jpms.utils";
  private static final String REQUIRED_ADD_OPENS_SUN_MANAGEMENT =
      "--add-opens=java.management/sun.management=org.mule.runtime.jpms.utils";
  private static final String REQUIRED_ADD_OPENS_COM_IBM_LANG_MANAGEMENT_INTERNAL =
      "--add-opens=jdk.management/com.ibm.lang.management.internal=org.mule.runtime.jpms.utils";
  private static final String REQUIRED_ADD_OPENS_COM_SUN_MANAGEMENT_INTERNAL =
      "--add-opens=jdk.management/com.sun.management.internal=org.mule.runtime.jpms.utils";

  private static final String REQUIRED_ADD_OPENS_BOUNCY_CASTLE_SECURE_RANDOM =
      "--add-opens=java.base/sun.security.provider=org.bouncycastle.fips.core";

  private static final List<String> REQUIRED_ADD_OPENS = asList(REQUIRED_ADD_OPENS_JAVA_LANG,
                                                                REQUIRED_ADD_OPENS_JAVA_LANG_REFLECT,
                                                                REQUIRED_ADD_OPENS_JAVA_LANG_INVOKE,
                                                                REQUIRED_ADD_OPENS_JDK_INTERNAL_REF,
                                                                REQUIRED_ADD_OPENS_JAVA_NIO,
                                                                REQUIRED_ADD_OPENS_SUN_NIO_CH,
                                                                REQUIRED_ADD_OPENS_JAVA_SQL,
                                                                REQUIRED_ADD_OPENS_SUN_MANAGEMENT,
                                                                REQUIRED_ADD_OPENS_COM_IBM_LANG_MANAGEMENT_INTERNAL,
                                                                REQUIRED_ADD_OPENS_COM_SUN_MANAGEMENT_INTERNAL,
                                                                REQUIRED_ADD_OPENS_BOUNCY_CASTLE_SECURE_RANDOM);

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

    doValidateArguments(getRuntimeMXBean().getInputArguments());
  }

  static void doValidateArguments(List<String> arguments) {
    final List<String> illegalAddModules = arguments
        .stream()
        .filter(arg -> arg.startsWith("--add-modules="))
        .flatMap(addModules -> Stream.of(addModules.split("=")[1].split(",")))
        .filter(moduleName -> REQUIRED_ADD_MODULES.stream().noneMatch(moduleName::equals))
        .collect(toList());

    if (!illegalAddModules.isEmpty()) {
      throw new IllegalArgumentException("Invalid module tweaking options passed to the JVM running the Mule Runtime: "
          + "--add-modules=" + illegalAddModules);
    }

    List<String> illegalArguments = arguments
        .stream()
        .filter(arg -> arg.startsWith("--add-exports=")
            || arg.startsWith("--add-opens=")
            || arg.startsWith("--add-reads=")
            || arg.startsWith("--patch-module="))
        .filter(not(REQUIRED_ADD_OPENS::contains))
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

          if (JAVA_MAJOR_VERSION >= 25) {
            return moduleName.startsWith("java.");
          } else {
            // Original intention is to only expose standard java modules...
            return moduleName.startsWith("java.")
                // ... however, the DB and SOAP Engine connectors, along with DataWeave, rely on an outdated version
                // of Caffeine (2.x) which introduces a dependency on sun.misc.unsafe, requiring jdk.* modules to be accessible.
                || moduleName.startsWith("jdk.");
          }
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

    final ModuleLayer layer = createModuleLayer(modulePathEntries, parent, emptyList(), false, true);
    return layer.findLoader(layer.modules().iterator().next().getName());
  }

  /**
   * Creates a {@link ModuleLayer} for the given {@code modulePathEntries} and with the given {@code parent}, and returns a
   * classLoader from which its modules can be read.
   *
   * @param modulePathEntries the URLs from which to find the modules
   * @param parent            the parent class loader for delegation
   * @param classes           classes from which to get the parent layers
   * @return a new classLoader.
   */
  public static ClassLoader createModuleLayerClassLoader(URL[] modulePathEntries, ClassLoader parent, List<Class<?>> classes) {
    if (!useModuleLayer()) {
      return new URLClassLoader(modulePathEntries, parent);
    }

    List<ModuleLayer> parentLayers = classes.stream().map(clazz -> clazz.getModule().getLayer()).collect(toList());
    final ModuleLayer layer = createModuleLayer(modulePathEntries, parent, parentLayers, false, true);

    openPackages(layer);

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

    List<ModuleLayer> resolvedParentLayers =
        clazz.map(cl -> cl.getModule().getLayer()).map(Collections::singletonList).orElse(emptyList());
    final ModuleLayer parentLayer =
        createModuleLayer(modulePathEntriesParent, parentClassLoader, resolvedParentLayers, false, true);
    ClassLoader childParentClassLoader = parentLayer.findLoader(parentLayer.modules().iterator().next().getName());
    final ModuleLayer childLayer =
        createModuleLayer(modulePathEntriesChild, childParentClassLoader, singletonList(parentLayer), false, true);
    openPackages(childLayer);

    return childLayer.findLoader(childLayer.modules().iterator().next().getName());
  }

  private static void openPackages(ModuleLayer layer) {
    openToModule(layer, MULE_LAUNCHER_MODULE_NAME, "org.mule.boot.api",
                 singletonList("org.mule.runtime.module.boot.internal"));
    openToModule(layer, KRYO_MODULE_NAME, "java.base",
                 asList("java.lang", "java.lang.reflect", "java.lang.invoke"));
    openToModule(layer, MULE_JPMS_UTILS_MODULE_NAME, "java.base",
                 asList("java.lang", "java.lang.reflect", "java.lang.invoke"));
    openToModule(layer, KRYO_MODULE_NAME, "java.sql",
                 asList("java.sql"));

    // To avoid a performance-related warning from Hazelcast according to
    // https://docs.hazelcast.com/hazelcast/5.2/getting-started/install-hazelcast#using-modular-java
    try {
      openToModule(layer, HAZELCAST_CORE_MODULE_NAME, "java.base",
                   asList("java.lang", "jdk.internal.ref", "java.nio", "sun.nio.ch"));
      openToModule(layer, HAZELCAST_CORE_MODULE_NAME, "jdk.management",
                   asList("com.sun.management.internal", "com.ibm.lang.management.internal"));
      openToModule(layer, HAZELCAST_CORE_MODULE_NAME, "java.management",
                   singletonList("sun.management"));
    } catch (IllegalCallerException e) {
      // It is fine to continue because the feature may not even be used
      // Logging here is possible, but it could be confusing for people not using clustering, a warning will be printed by
      // Hazelcast
    }
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
   * @param filterParentModules                whether modules already present in parent layers should be removed from the given
   *                                           {@code modulePathEntries}.
   * @return a new {@link ModuleLayer}.
   */
  public static ModuleLayer createModuleLayer(URL[] modulePathEntries, ClassLoader parent, Optional<ModuleLayer> parentLayer,
                                              boolean isolateDependenciesInTheirOwnLayer,
                                              boolean filterParentModules) {
    return createModuleLayer(modulePathEntries, parent, parentLayer.map(Collections::singletonList).orElse(emptyList()),
                             isolateDependenciesInTheirOwnLayer, filterParentModules);
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
   * @param parentLayers                       layers of modules that will be visible from the newly created {@link ModuleLayer}.
   * @param isolateDependenciesInTheirOwnLayer whether an additional {@link ModuleLayer} having only the {@code boot} layer as
   *                                           parent will be created for modules that need to be isolated.
   * @param filterParentModules                whether modules already present in parent layers should be removed from the given
   *                                           {@code modulePathEntries}.
   * @return a new {@link ModuleLayer}.
   */
  public static ModuleLayer createModuleLayer(URL[] modulePathEntries, ClassLoader parent, List<ModuleLayer> parentLayers,
                                              boolean isolateDependenciesInTheirOwnLayer,
                                              boolean filterParentModules) {
    List<ModuleLayer> resolvedParentLayers = new ArrayList<>(parentLayers);
    if (resolvedParentLayers.isEmpty()) {
      resolvedParentLayers.add(boot());
    }

    final Set<String> modulesToFilter;
    if (filterParentModules) {
      List<ModuleLayer> layers = isolateDependenciesInTheirOwnLayer ? singletonList(boot()) : resolvedParentLayers;
      Set<Module> parentLayersModules = getParentLayersModules(layers);
      modulesToFilter = parentLayersModules.stream()
          .map(Module::getName)
          .collect(toSet());
    } else {
      modulesToFilter = emptySet();
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

    final Set<String> muleContainerModuleNames = getMuleContainerModuleNames(parentLayers);

    Map<Boolean, List<ModuleReference>> modulesByIsolation = modulesFinder
        .findAll()
        .stream()
        .filter(moduleRef -> !modulesToFilter.contains(moduleRef.descriptor().name()))
        .collect(partitioningBy(moduleRef -> isolateInOrphanLayer(moduleRef, muleContainerModuleNames)));

    Controller controller;
    if (isolateDependenciesInTheirOwnLayer) {
      // put all modules requiring isolation in their own layer, having only boot layer as parent...
      Path[] isolatedModulesPaths = modulesByIsolation.get(true)
          .stream()
          .map(ModuleReference::location)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .map(Paths::get)
          .toArray(size -> new Path[size]);
      ModuleFinder isolatedModulesFinder = ModuleFinder.of(isolatedModulesPaths);

      final List<String> isolatedRoots = modulesByIsolation.get(true)
          .stream()
          .map(moduleRef -> moduleRef.descriptor().name())
          .collect(toList());
      Configuration isolatedModulesConfiguration = boot().configuration()
          .resolve(isolatedModulesFinder, ofSystem(), isolatedRoots);
      Controller isolatedModulesController = defineModulesWithOneLoader(isolatedModulesConfiguration,
                                                                        singletonList(boot()),
                                                                        parent);

      // ... the put the rest of the modules on a new layer with the isolated modules one as parent.
      Path[] notIsolatedModulesPaths = modulesByIsolation.get(false)
          .stream()
          .map(ModuleReference::location)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .map(Paths::get)
          .toArray(size -> new Path[size]);
      ModuleFinder notIsolatedModulesFinder = ModuleFinder.of(notIsolatedModulesPaths);

      final List<String> notIsolatedRoots = modulesByIsolation.get(false).stream()
          .map(moduleRef -> moduleRef.descriptor().name())
          .collect(toList());
      List<Configuration> parentConfigurations = new ArrayList<>();
      parentConfigurations.add(isolatedModulesController.layer().configuration());
      parentConfigurations.addAll(resolvedParentLayers.stream().map(ModuleLayer::configuration).collect(toList()));
      Configuration configuration = resolve(notIsolatedModulesFinder,
                                            parentConfigurations,
                                            ofSystem(),
                                            notIsolatedRoots);
      List<ModuleLayer> layers = new ArrayList<>();
      layers.add(isolatedModulesController.layer());
      layers.addAll(resolvedParentLayers);
      controller = defineModulesWithOneLoader(configuration,
                                              layers,
                                              parent);

    } else {
      Path[] filteredModulesPaths = modulesByIsolation.values().stream()
          .flatMap(Collection::stream)
          .map(ModuleReference::location)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .map(Paths::get)
          .toArray(size -> new Path[size]);
      ModuleFinder filteredModulesFinder = ModuleFinder.of(filteredModulesPaths);

      final List<String> roots = modulesByIsolation.values().stream()
          .flatMap(Collection::stream)
          .map(moduleRef -> moduleRef.descriptor().name())
          .collect(toList());
      Configuration configuration = resolve(filteredModulesFinder,
                                            resolvedParentLayers.stream().map(ModuleLayer::configuration).collect(toList()),
                                            ofSystem(),
                                            roots);
      controller = defineModulesWithOneLoader(configuration,
                                              resolvedParentLayers,
                                              parent);
    }


    return controller.layer();
  }

  private static Set<String> getMuleContainerModuleNames(List<ModuleLayer> containerLayers) {
    return getParentLayersModules(containerLayers).stream().map(Module::getName)
        .filter(containerModuleName -> containerModuleName.startsWith("org.mule")
            || containerModuleName.startsWith("com.mulesoft"))
        .collect(toSet());
  }

  private static Set<Module> getParentLayersModules(List<ModuleLayer> moduleLayers) {
    Set<Module> modules = new HashSet<>();

    for (ModuleLayer layer : moduleLayers) {
      getParentLayersModules(layer, modules);
    }

    return modules;
  }

  private static Set<Module> getParentLayersModules(ModuleLayer moduleLayer, Set<Module> modules) {
    modules.addAll(moduleLayer.modules());
    for (ModuleLayer parent : moduleLayer.parents()) {
      modules.addAll(getParentLayersModules(parent, modules));
    }

    return modules;
  }

  /**
   *
   * @param moduleRef
   * @param containerModuleNames
   * @return {@code true} the the referenced module is automatic or does not read any module from the container.
   */
  private static boolean isolateInOrphanLayer(ModuleReference moduleRef, Set<String> muleContainerModuleNames) {
    if (moduleRef.descriptor().isAutomatic()
        // TODO W-13761983 remove this condition
        && SERVICE_MODULE_NAME_PREFIXES.stream()
            .noneMatch(moduleRef.descriptor().name()::startsWith)) {
      return true;
    }

    return moduleRef.descriptor().requires().stream()
        .noneMatch(req -> muleContainerModuleNames.contains(req.name()));
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
    final Class<?> callerClass = StackWalker.getInstance(RETAIN_CLASS_REFERENCE).getCallerClass();
    final String callerClassName = callerClass.getName();
    if (!(callerClassName.equals("org.mule.runtime.module.service.api.artifact.ServiceModuleLayerFactory")
        || callerClassName.equals("org.mule.runtime.jpms.api.JpmsUtils"))) {
      throw new UnsupportedOperationException("This is for internal use only.");
    }

    Module callerModule = getCallerModule(callerClass);
    layer.findModule(moduleName).filter(module -> module != callerModule)
        .ifPresent(module -> boot().findModule(bootModuleName)
            .ifPresent(bootModule -> {
              for (String pkg : packages) {
                if (bootModule.getPackages().contains(pkg)) {
                  bootModule.addOpens(pkg, module);
                }
              }
            }));
  }

  /**
   * Returns the module that a given caller class is a member of. Returns {@code null} if the caller is {@code null}.
   */
  private static Module getCallerModule(Class<?> caller) {
    return (caller != null) ? caller.getModule() : null;
  }

}
