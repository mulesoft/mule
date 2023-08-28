/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.launcher.coreextension;

import org.mule.runtime.container.api.MuleCoreExtension;

import java.util.Collection;
import java.util.List;

/**
 * Resolves dependencies between {@link MuleCoreExtension} instances.
 */
public interface MuleCoreExtensionDependencyResolver {

  /**
   * Resolves the dependencies between a given collection of {link MuleCoreExtension}
   *
   * @param coreExtensions core extensions to resolve
   * @return a list of core extensions ordered putting the independent extensions first and the most dependent ones last.
   * @throws UnresolveableDependencyException when dependencies cannot be successfully resolved
   */
  List<MuleCoreExtension> resolveDependencies(Collection<MuleCoreExtension> coreExtensions);
}
