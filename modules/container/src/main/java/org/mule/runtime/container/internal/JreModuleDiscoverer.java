/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.internal;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.container.internal.JreExplorer.exploreJdk;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.module.artifact.api.classloader.ExportedService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.slf4j.Logger;

/**
 * Discovers the module corresponding to the JRE by creating a new {@link MuleModule} with the Java packages, resources and
 * services found on the used JRE.
 *
 * @since 4.0
 */
public class JreModuleDiscoverer implements ModuleDiscoverer {

  private static final Logger logger = getLogger(JreModuleDiscoverer.class);

  protected static final String JRE_MODULE_NAME = "jre";

  private static final Supplier<List<MuleModule>> jreModulesSupplier = new LazyValue<>(() -> {
    Set<String> packages = new HashSet<>(1024);
    Set<String> resources = new HashSet<>(1024);
    List<ExportedService> services = new ArrayList<>(128);

    exploreJdk(packages, resources, services);

    if (logger.isDebugEnabled()) {
      logger.debug("Discovered JRE:\npackages: {}\nresources: {}\nservices: {}", packages, resources,
                   services.stream().map(p -> p.getServiceInterface() + ":" + p.getResource().toString()).collect(toList()));
    }

    MuleModule jdkModule = new MuleModule(JRE_MODULE_NAME, packages, resources, emptySet(), emptySet(), services);

    return singletonList(jdkModule);
  });

  @Override
  public List<MuleModule> discover() {
    return jreModulesSupplier.get();
  }
}
