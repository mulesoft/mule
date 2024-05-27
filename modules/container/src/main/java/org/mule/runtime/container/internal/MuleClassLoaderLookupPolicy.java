/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.internal;

import static org.mule.runtime.api.util.collection.SmallMap.copy;
import static org.mule.runtime.module.artifact.api.classloader.ChildFirstLookupStrategy.CHILD_FIRST;
import static org.mule.runtime.module.artifact.api.classloader.ParentOnlyLookupStrategy.PARENT_ONLY;

import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;

import static org.apache.commons.lang3.ClassUtils.getPackageName;

import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Defines which resources in a class loader should be looked up using parent-first, parent-only or child-first strategies.
 * <p/>
 * Default lookup strategy is child first. To use parent-first, the corresponding package must be added as an overridden. To use
 * parent-only, the corresponding package must be added as blocked.
 */
public class MuleClassLoaderLookupPolicy implements ClassLoaderLookupPolicy {

  private static final String PACKAGE_SEPARATOR = ".";

  private final Map<String, LookupStrategy> configuredLookupStrategies;
  private final Set<String> rootSystemPackages;
  private final Map<String, LookupStrategy> lookupStrategies;

  /**
   * Creates a new lookup policy based on the provided configuration.
   *
   * @param lookupStrategies   lookup strategy to use with specific packages. Non null.
   * @param rootSystemPackages packages that must use {@link ContainerOnlyLookupStrategy}. Any inner package extending from a
   *                           system package root will use the same approach.
   */
  public MuleClassLoaderLookupPolicy(Map<String, LookupStrategy> lookupStrategies, Set<String> rootSystemPackages) {
    requireNonNull(lookupStrategies, "Lookup strategies cannot be null");
    requireNonNull(rootSystemPackages, "System packages cannot be null");
    this.rootSystemPackages = normalizeRootSystemPackages(rootSystemPackages);
    this.configuredLookupStrategies = normalizeLookupStrategies(lookupStrategies);
    this.lookupStrategies = copy(configuredLookupStrategies);
  }

  private Map<String, LookupStrategy> normalizeLookupStrategies(Map<String, LookupStrategy> lookupStrategies) {
    final Map<String, LookupStrategy> result = new HashMap<>();

    for (String packageName : lookupStrategies.keySet()) {
      result.put(normalizePackageName(packageName), lookupStrategies.get(packageName));
    }

    return result;
  }

  private void validateLookupPolicies(Map<String, LookupStrategy> lookupStrategies) {
    for (String packageName : lookupStrategies.keySet()) {
      validateLookupPolicy(packageName, lookupStrategies.get(packageName));
    }
  }

  private void validateLookupPolicy(String packageName, LookupStrategy lookupStrategy) {
    if (isSystemPackage(packageName) && !(lookupStrategy instanceof ContainerOnlyLookupStrategy)) {
      throw new IllegalArgumentException(invalidLookupPolicyOverrideError(packageName, lookupStrategy));
    }
  }

  protected static String invalidLookupPolicyOverrideError(String packageName, LookupStrategy lookupStrategy) {
    return "Attempt to override lookup strategy " + lookupStrategy.getClass().getSimpleName() + " for package: " + packageName;
  }

  private String normalizePackageName(String packageName) {
    if (packageName.endsWith(".")) {
      packageName = packageName.substring(0, packageName.length() - 1);
    }
    return packageName;
  }

  private Set<String> normalizeRootSystemPackages(Set<String> rootSystemPackages) {
    final HashSet<String> result = new HashSet<>();
    for (String systemPackage : rootSystemPackages) {
      systemPackage = systemPackage.trim();
      if (!systemPackage.endsWith(PACKAGE_SEPARATOR)) {
        systemPackage = systemPackage + PACKAGE_SEPARATOR;
      }
      result.add(systemPackage);
    }

    return result;
  }

  @Override
  public LookupStrategy getClassLookupStrategy(String className) {
    return getPackageLookupStrategy(getPackageName(className));
  }

  @Override
  public LookupStrategy getPackageLookupStrategy(String packageName) {
    LookupStrategy lookupStrategy = lookupStrategies.get(packageName);
    if (lookupStrategy == null) {
      synchronized (this) {
        lookupStrategy = lookupStrategies.get(packageName);
        if (lookupStrategy == null) {
          if (isSystemPackage(packageName)) {
            lookupStrategy = PARENT_ONLY;
          } else {
            lookupStrategy = CHILD_FIRST;
          }
          lookupStrategies.put(packageName, lookupStrategy);
        }
      }
    }

    return lookupStrategy;
  }

  @Override
  public ClassLoaderLookupPolicy extend(Map<String, LookupStrategy> lookupStrategies) {
    return extend(lookupStrategies, false);
  }

  @Override
  public ClassLoaderLookupPolicy extend(Stream<String> packages, LookupStrategy lookupStrategy) {
    return extend(packages, lookupStrategy, false);
  }

  @Override
  public ClassLoaderLookupPolicy extend(Map<String, LookupStrategy> lookupStrategies, boolean overwrite) {
    validateLookupPolicies(lookupStrategies);
    final Map<String, LookupStrategy> newLookupStrategies = copy(this.configuredLookupStrategies);

    for (String packageName : lookupStrategies.keySet()) {
      if (overwrite || !newLookupStrategies.containsKey(normalizePackageName(packageName))) {
        newLookupStrategies.put(packageName, lookupStrategies.get(packageName));
      }
    }

    final MuleClassLoaderLookupPolicy muleClassLoaderLookupPolicy =
        new MuleClassLoaderLookupPolicy(newLookupStrategies, rootSystemPackages);

    return muleClassLoaderLookupPolicy;
  }

  @Override
  public ClassLoaderLookupPolicy extend(Stream<String> packages, LookupStrategy lookupStrategy, boolean overwrite) {
    final Map<String, LookupStrategy> newLookupStrategies = copy(this.configuredLookupStrategies);

    packages.forEach(packageName -> {
      validateLookupPolicy(packageName, lookupStrategy);
      if (overwrite || !newLookupStrategies.containsKey(normalizePackageName(packageName))) {
        newLookupStrategies.put(packageName, lookupStrategy);
      }
    });

    final MuleClassLoaderLookupPolicy muleClassLoaderLookupPolicy =
        new MuleClassLoaderLookupPolicy(newLookupStrategies, rootSystemPackages);

    return muleClassLoaderLookupPolicy;
  }

  private boolean isSystemPackage(String packageName) {
    return rootSystemPackages.contains(packageName + PACKAGE_SEPARATOR)
        || rootSystemPackages.contains(packageName);
  }

  @Override
  public String toString() {
    return "MuleClassLoaderLookupPolicy{" + lineSeparator() +
        "\tconfiguredLookupStrategies: " + configuredLookupStrategies + lineSeparator() +
        "\trootSystemPackages: " + rootSystemPackages + lineSeparator() +
        "\tlookupStrategies: " + lookupStrategies + lineSeparator() +
        "}";
  }
}
