/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.core.policy.PolicyParametrization;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplate;
import org.mule.runtime.module.artifact.classloader.ClassLoaderRepository;
import org.mule.runtime.module.service.ServiceRepository;

/**
 * Creates instances of {@link DefaultPolicyInstanceProvider}
 */
public class DefaultPolicyInstanceProviderFactory implements PolicyInstanceProviderFactory {

  private final ServiceRepository serviceRepository;
  private final ClassLoaderRepository classLoaderRepository;

  /**
   * Creates a new factory
   *
   * @param serviceRepository contains available service instances. Non null.
   * @param classLoaderRepository contains the registered classloaders that can be used to load serialized classes. Non null.
   */
  public DefaultPolicyInstanceProviderFactory(ServiceRepository serviceRepository, ClassLoaderRepository classLoaderRepository) {
    checkArgument(serviceRepository != null, "serviceRepository cannot be null");
    checkArgument(classLoaderRepository != null, "classLoaderRepository cannot be null");

    this.serviceRepository = serviceRepository;
    this.classLoaderRepository = classLoaderRepository;
  }

  @Override
  public PolicyInstanceProvider create(Application application, PolicyTemplate policyTemplate,
                                       PolicyParametrization parametrization) {
    return new DefaultPolicyInstanceProvider(application, policyTemplate, parametrization, serviceRepository,
                                             classLoaderRepository);
  }

}
