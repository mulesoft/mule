/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.service.internal.discoverer;


import org.mule.runtime.api.service.Service;
import org.mule.runtime.module.service.api.discoverer.ServiceAssembly;

import java.util.List;

/**
 * Resolves all the {@link Service services} provided by the available {@link ServiceAssembly assemblies}.
 */
public interface ServiceResolver {

  /**
   * Resolves the services instances provided by the given assemblies.
   *
   * @param assemblies service assemblies to be resolved. Non null.
   * @return A list of {@link Service services}
   */
  List<Service> resolveServices(List<ServiceAssembly> assemblies);
}
