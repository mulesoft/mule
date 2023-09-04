/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.jpms.api;

import static java.util.Collections.emptySet;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;

import java.lang.module.ModuleDescriptor.Exports;
import java.util.Set;


public class MuleJpmsModule implements MuleContainerModule {

  private final String name;
  private final Set<String> exportedPackages;

  public MuleJpmsModule(Module muleModule) {
    name = muleModule.getDescriptor().name();

    exportedPackages = muleModule.getDescriptor().exports()
        .stream()
        .filter(not(Exports::isQualified))
        .map(Exports::source)
        .collect(toSet());
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Set<String> getExportedPackages() {
    return exportedPackages;
  }

  @Override
  public Set<String> getExportedPaths() {
    return emptySet();
  }

  @Override
  public Set<String> getPrivilegedExportedPackages() {
    return emptySet();
  }

  @Override
  public Set<String> getPrivilegedArtifacts() {
    return emptySet();
  }

}
