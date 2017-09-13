/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.internal;

import static org.apache.commons.lang3.ClassUtils.getPackageName;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.artifact.api.classloader.ChildFirstLookupStrategy.CHILD_FIRST;
import static org.mule.runtime.module.artifact.api.classloader.ParentOnlyLookupStrategy.PARENT_ONLY;

import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;

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

  private final Map<String, LookupStrategy> configuredLookupStrategies;
  private final Set<String> rootSystemPackages;
  private final Map<String, LookupStrategy> lookupStrategies;

  /**
   * Creates a new lookup policy based on the provided configuration.
   *
   * @param lookupStrategies lookup strategy to use with specific packages. Non null.
   * @param rootSystemPackages packages that must use {@link ContainerOnlyLookupStrategy}. Any inner package extending
   *        from a system package root will use the same approach.
   */
  public MuleClassLoaderLookupPolicy(Map<String, LookupStrategy> lookupStrategies, Set<String> rootSystemPackages) {
    checkArgument(lookupStrategies != null, "Lookup strategies cannot be null");
    checkArgument(rootSystemPackages != null, "System packages cannot be null");
    this.rootSystemPackages = normalizeRootSystemPackages(rootSystemPackages);
    this.configuredLookupStrategies = normalizeLookupStrategies(lookupStrategies);
    this.lookupStrategies = new HashMap<>(configuredLookupStrategies);
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
      if (isSystemPackage(packageName) && !(lookupStrategies.get(packageName) instanceof ContainerOnlyLookupStrategy)) {
        throw new IllegalArgumentException(invalidLookupPolicyOverrideError(packageName, lookupStrategies.get(packageName)));
      }
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
    validateLookupPolicies(lookupStrategies);
    final Map<String, LookupStrategy> newLookupStrategies = new HashMap<>(this.configuredLookupStrategies);

    for (String packageName : lookupStrategies.keySet()) {
      if (!newLookupStrategies.containsKey(normalizePackageName(packageName))) {
        newLookupStrategies.put(packageName, lookupStrategies.get(packageName));
      }
    }

    final MuleClassLoaderLookupPolicy muleClassLoaderLookupPolicy =
        new MuleClassLoaderLookupPolicy(newLookupStrategies, rootSystemPackages);

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
