/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.classloader;

import static org.apache.commons.lang.ClassUtils.getPackageName;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.CHILD_FIRST;
import static org.mule.runtime.module.artifact.classloader.ClassLoaderLookupStrategy.PARENT_ONLY;
import static org.mule.runtime.core.util.Preconditions.checkArgument;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Defines which resources in a class loader should be looked up using parent-first, parent-only or child-first strategies.
 * <p/>
 * Default lookup strategy is child first. To use parent-first, the corresponding package must be added as an overridden. To use
 * parent-only, the corresponding package must be added as blocked.
 */
public class MuleClassLoaderLookupPolicy implements ClassLoaderLookupPolicy {

  private static final String PACKAGE_SEPARATOR = ".";

  private final Map<String, ClassLoaderLookupStrategy> configuredlookupStrategies;
  private final Set<String> rootSystemPackages;
  private final HashMap<String, ClassLoaderLookupStrategy> lookupStrategies;

  /**
   * Creates a new lookup policy based on the provided configuration.
   *
   * @param lookupStrategies lookup strategy to use with specific packages. Non null.
   * @param rootSystemPackages packages that must use {@link ClassLoaderLookupStrategy#PARENT_ONLY}. Any inner package extending
   *        from a system package root will use the same approach.
   */
  public MuleClassLoaderLookupPolicy(Map<String, ClassLoaderLookupStrategy> lookupStrategies, Set<String> rootSystemPackages) {
    checkArgument(lookupStrategies != null, "Lookup strategies cannot be null");
    checkArgument(rootSystemPackages != null, "System packages cannot be null");
    this.rootSystemPackages = normalizeRootSystemPackages(rootSystemPackages);
    this.configuredlookupStrategies = normalizeLookupStrategies(lookupStrategies);
    this.lookupStrategies = new HashMap<>(configuredlookupStrategies);
  }

  private Map<String, ClassLoaderLookupStrategy> normalizeLookupStrategies(Map<String, ClassLoaderLookupStrategy> lookupStrategies) {
    final Map<String, ClassLoaderLookupStrategy> result = new HashMap<>();

    for (String packageName : lookupStrategies.keySet()) {
      result.put(normalizePackageName(packageName), lookupStrategies.get(packageName));
    }

    return result;
  }

  private void validateLookupPolicies(Map<String, ClassLoaderLookupStrategy> lookupStrategies) {
    for (String packageName : lookupStrategies.keySet()) {
      if (isSystemPackage(packageName)) {
        throw new IllegalArgumentException("Attempt to override lookup strategy for system package: " + packageName);
      }
    }
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
  public ClassLoaderLookupStrategy getLookupStrategy(String className) {
    final String packageName = getPackageName(className);

    ClassLoaderLookupStrategy lookupStrategy = lookupStrategies.get(packageName);
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
  public ClassLoaderLookupPolicy extend(Map<String, ClassLoaderLookupStrategy> lookupStrategies) {
    validateLookupPolicies(lookupStrategies);
    final HashMap<String, ClassLoaderLookupStrategy> newLookupStraetgies = new HashMap<>(this.configuredlookupStrategies);

    for (String packageName : lookupStrategies.keySet()) {
      if (!newLookupStraetgies.containsKey(normalizePackageName(packageName))) {
        newLookupStraetgies.put(packageName, lookupStrategies.get(packageName));
      }
    }

    final MuleClassLoaderLookupPolicy muleClassLoaderLookupPolicy =
        new MuleClassLoaderLookupPolicy(newLookupStraetgies, rootSystemPackages);

    return muleClassLoaderLookupPolicy;
  }

  private boolean isSystemPackage(String packageName) {
    packageName = packageName + PACKAGE_SEPARATOR;

    for (String systemPackage : rootSystemPackages) {
      if (packageName.startsWith(systemPackage)) {
        return true;
      }
    }

    return false;
  }
}
