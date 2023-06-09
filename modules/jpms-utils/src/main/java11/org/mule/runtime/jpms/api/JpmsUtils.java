/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.jpms.api;

import static java.lang.Boolean.getBoolean;
import static java.lang.ModuleLayer.boot;
import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;
import static java.util.stream.Collectors.toList;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Set;

/**
 * Utilities related to how Mule uses the Java Module system.
 * 
 * @since 4.5
 */
public final class JpmsUtils {

  private JpmsUtils() {
    // Nothing to do
  }

  public static final String MULE_SKIP_MODULE_TWEAKING_VALIDATION = "mule.module.tweaking.validation.skip";

  private static final String REQUIRED_ADD_MODULES =
      "--add-modules="
          + "java.se,"
          + "org.mule.runtime.jpms.utils,"
          + "com.fasterxml.jackson.core";
  private static final String REQUIRED_CE_BOOT_ADD_EXPORTS =
      "--add-exports=org.mule.boot/org.mule.runtime.module.reboot=ALL-UNNAMED";
  private static final String REQUIRED_BOOT_ADD_EXPORTS =
      "--add-exports=com.mulesoft.mule.boot/org.mule.runtime.module.reboot=ALL-UNNAMED";
  private static final String REQUIRED_ADD_OPENS =
      "--add-opens=java.base/java.lang=org.mule.runtime.jpms.utils";

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
            || arg.equals(REQUIRED_CE_BOOT_ADD_EXPORTS)
            || arg.equals(REQUIRED_BOOT_ADD_EXPORTS)
            || arg.equals(REQUIRED_ADD_OPENS)))
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
              // ... but because we need to keep compatibility with Java 8, older verion of some libraries use internal jdk
              // modules packages:
              // * caffeine 2.x still uses sun.misc.Unsafe. Ref: https://github.com/ben-manes/caffeine/issues/273
              // * obgenesis SunReflectionFactoryHelper, used by Mockito
              || moduleName.startsWith("jdk.");
        })
        .forEach(module -> packages.addAll(module.getPackages()));
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
    if (!StackWalker.getInstance(RETAIN_CLASS_REFERENCE).getCallerClass().getName()
        .equals("org.mule.runtime.module.service.api.artifact.ServiceModuleLayerFactory")) {
      throw new UnsupportedOperationException("This is for internal use only.");
    }

    layer.findModule(moduleName)
        .ifPresent(module -> {
          Module bootModule = ModuleLayer.boot()
              .findModule(bootModuleName).get();

          for (String pkg : packages) {
            bootModule.addOpens(pkg, module);
          }
        });
  }

}
