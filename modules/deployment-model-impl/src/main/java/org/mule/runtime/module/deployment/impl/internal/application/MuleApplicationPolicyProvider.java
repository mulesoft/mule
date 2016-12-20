/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.application;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.policy.PolicyParametrization;
import org.mule.runtime.core.policy.PolicyPointcutParameters;
import org.mule.runtime.core.policy.PolicyProvider;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplate;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyInstance;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyInstanceFactory;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Provides policy management and provision for Mule applications
 */
public class MuleApplicationPolicyProvider implements ApplicationPolicyProvider, PolicyProvider, Disposable {

  private final PolicyTemplateFactory policyTemplateFactory;
  private final PolicyInstanceFactory policyInstanceFactory;
  private final List<PolicyTemplate> registeredPolicyTemplates = new LinkedList<>();
  private final List<PolicyInstance> registeredPolicyInstances = new LinkedList<>();
  private Application application;

  /**
   * Creates a new provider
   *
   * @param policyTemplateFactory used to create the policy templates for the application. Non null.
   * @param policyInstanceFactory used to create the policy instances for the application. Non null.
   */
  public MuleApplicationPolicyProvider(PolicyTemplateFactory policyTemplateFactory, PolicyInstanceFactory policyInstanceFactory) {
    this.policyTemplateFactory = policyTemplateFactory;
    this.policyInstanceFactory = policyInstanceFactory;
  }

  @Override
  public void addPolicy(PolicyTemplateDescriptor policyTemplateDescriptor, PolicyParametrization parametrization) {
    checkArgument(application != null, "application was not configured on the policy provider");

    PolicyTemplate policyTemplate =
        policyTemplateFactory.createArtifact(policyTemplateDescriptor, application.getRegionClassLoader());
    registeredPolicyTemplates.add(policyTemplate);

    PolicyInstance policyInstance = policyInstanceFactory.create(application, policyTemplate, parametrization);
    registeredPolicyInstances.add(policyInstance);
  }

  @Override
  public List<org.mule.runtime.core.policy.Policy> findSourceParameterizedPolicies(
                                                                                   PolicyPointcutParameters policyPointcutParameters) {
    List<org.mule.runtime.core.policy.Policy> policies = new ArrayList<>();

    if (!registeredPolicyInstances.isEmpty()) {
      for (PolicyInstance policyInstance : registeredPolicyInstances) {
        if (policyInstance.getPointcut().matches(policyPointcutParameters)) {
          policies.addAll(policyInstance.findSourceParameterizedPolicies(policyPointcutParameters));
        }
      }
    }

    return policies;
  }

  @Override
  public List<org.mule.runtime.core.policy.Policy> findOperationParameterizedPolicies(

                                                                                      PolicyPointcutParameters policyPointcutParameters) {
    List<org.mule.runtime.core.policy.Policy> policies = new ArrayList<>();

    if (!registeredPolicyInstances.isEmpty()) {
      for (PolicyInstance policyInstance : registeredPolicyInstances) {
        if (policyInstance.getPointcut().matches(policyPointcutParameters)) {
          policies.addAll(policyInstance.findOperationParameterizedPolicies(policyPointcutParameters));
        }
      }
    }

    return policies;
  }

  @Override
  public void dispose() {

    for (PolicyInstance registeredPolicyInstance : registeredPolicyInstances) {
      registeredPolicyInstance.dispose();
    }

    for (PolicyTemplate registeredPolicyTemplate : registeredPolicyTemplates) {
      try {
        registeredPolicyTemplate.dispose();
      } catch (RuntimeException e) {
        // Ignore and continue
      }

      registeredPolicyTemplates.clear();
    }
  }

  public void setApplication(Application application) {
    this.application = application;
  }
}
