/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.core.api.policy.PolicyParametrization;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplate;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderRepository;
import org.mule.runtime.module.service.api.manager.ServiceRepository;

/**
 * Creates instances of {@link DefaultApplicationPolicyInstance}
 */
public class DefaultPolicyInstanceProviderFactory implements PolicyInstanceProviderFactory {

  private final ServiceRepository serviceRepository;
  private final ClassLoaderRepository classLoaderRepository;
  private final ExtensionModelLoaderRepository extensionModelLoaderRepository;

  /**
   * Creates a new factory
   *
   * @param serviceRepository contains available service instances. Non null.
   * @param classLoaderRepository contains the registered classloaders that can be used to load serialized classes. Non null.
   * @param extensionModelLoaderRepository {@link ExtensionModelLoaderRepository} with the available extension loaders. Non null.
   */
  public DefaultPolicyInstanceProviderFactory(ServiceRepository serviceRepository, ClassLoaderRepository classLoaderRepository,
                                              ExtensionModelLoaderRepository extensionModelLoaderRepository) {
    this.extensionModelLoaderRepository = extensionModelLoaderRepository;
    checkArgument(serviceRepository != null, "serviceRepository cannot be null");
    checkArgument(classLoaderRepository != null, "classLoaderRepository cannot be null");
    checkArgument(extensionModelLoaderRepository != null, "extensionModelLoaderRepository cannot be null");

    this.serviceRepository = serviceRepository;
    this.classLoaderRepository = classLoaderRepository;
  }

  @Override
  public ApplicationPolicyInstance create(Application application, PolicyTemplate policyTemplate,
                                          PolicyParametrization parametrization) {
    return new DefaultApplicationPolicyInstance(application, policyTemplate, parametrization, serviceRepository,
                                                classLoaderRepository, policyTemplate.getArtifactPlugins(),
                                                extensionModelLoaderRepository);
  }

}
