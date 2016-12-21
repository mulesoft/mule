/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import static java.lang.String.format;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter.NULL_CLASSLOADER_FILTER;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplate;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.RegionClassLoader;

import java.io.IOException;

/**
 * Creates {@link DefaultPolicyTemplate} instances.
 */
public class DefaultPolicyTemplateFactory implements PolicyTemplateFactory {

  private final PolicyTemplateClassLoaderBuilderFactory policyTemplateClassLoaderBuilderFactory;

  /**
   * Creates a new factory
   *
   * @param policyTemplateClassLoaderBuilderFactory creates class loader builders to create the class loaders for the created policy templates. Non null.
   */
  public DefaultPolicyTemplateFactory(PolicyTemplateClassLoaderBuilderFactory policyTemplateClassLoaderBuilderFactory) {
    checkArgument(policyTemplateClassLoaderBuilderFactory != null, "policyTemplateClassLoaderBuilderFactory cannot be null");

    this.policyTemplateClassLoaderBuilderFactory = policyTemplateClassLoaderBuilderFactory;
  }

  @Override
  public PolicyTemplate createArtifact(PolicyTemplateDescriptor descriptor, RegionClassLoader regionClassLoader) {
    ArtifactClassLoader policyClassLoader;
    try {
      policyClassLoader = policyTemplateClassLoaderBuilderFactory.createArtifactClassLoaderBuilder()
          .setParentClassLoader(regionClassLoader).setArtifactDescriptor(descriptor).build();
    } catch (IOException e) {
      throw new PolicyTemplateCreationException(createPolicyTemplateCreationErrorMessage(descriptor.getName()), e);
    }
    regionClassLoader.addClassLoader(policyClassLoader, NULL_CLASSLOADER_FILTER);

    DefaultPolicyTemplate policy =
        new DefaultPolicyTemplate(policyClassLoader.getArtifactId(), descriptor, policyClassLoader);

    return policy;
  }

  /**
   * @param policyName name of the policy that cannot be created
   * @return the error message to indicate policy creation failure
   */
  static String createPolicyTemplateCreationErrorMessage(String policyName) {
    return format("Cannot create policy template '%s'", policyName);
  }
}
