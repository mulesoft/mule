/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.application;

import static java.lang.Integer.compare;
import static java.lang.String.format;
import static java.util.Optional.of;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyParametrization;
import org.mule.runtime.core.api.policy.PolicyProvider;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.policy.PolicyRegistrationException;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplate;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.deployment.impl.internal.policy.ApplicationPolicyInstance;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyInstanceProviderFactory;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateFactory;
import org.mule.runtime.policy.api.PolicyPointcutParameters;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Provides policy management and provision for Mule applications
 */
public class MuleApplicationPolicyProvider implements ApplicationPolicyProvider, PolicyProvider, Disposable {

  private final PolicyTemplateFactory policyTemplateFactory;
  private final PolicyInstanceProviderFactory policyInstanceProviderFactory;
  private final List<RegisteredPolicyTemplate> registeredPolicyTemplates = new LinkedList<>();
  private final List<RegisteredPolicyInstanceProvider> registeredPolicyInstanceProviders = new LinkedList<>();
  private Application application;

  /**
   * Creates a new provider
   *
   * @param policyTemplateFactory used to create the policy templates for the application. Non null.
   * @param policyInstanceProviderFactory used to create the policy instances for the application. Non null.
   */
  public MuleApplicationPolicyProvider(PolicyTemplateFactory policyTemplateFactory,
                                       PolicyInstanceProviderFactory policyInstanceProviderFactory) {
    this.policyTemplateFactory = policyTemplateFactory;
    this.policyInstanceProviderFactory = policyInstanceProviderFactory;
  }

  @Override
  public synchronized void addPolicy(PolicyTemplateDescriptor policyTemplateDescriptor, PolicyParametrization parametrization)
      throws PolicyRegistrationException {
    try {
      checkArgument(application != null, "application was not configured on the policy provider");

      Optional<RegisteredPolicyInstanceProvider> registeredPolicyInstanceProvider = registeredPolicyInstanceProviders.stream()
          .filter(p -> p.getPolicyId().equals(parametrization.getId())).findFirst();
      if (registeredPolicyInstanceProvider.isPresent()) {
        throw new IllegalArgumentException(createPolicyAlreadyRegisteredError(parametrization.getId()));
      }

      Optional<RegisteredPolicyTemplate> registeredPolicyTemplate = registeredPolicyTemplates.stream()
          .filter(p -> p.policyTemplate.getDescriptor().getBundleDescriptor().getArtifactId()
              .equals(policyTemplateDescriptor.getBundleDescriptor().getArtifactId()))
          .findAny();

      if (!registeredPolicyTemplate.isPresent()) {
        PolicyTemplate policyTemplate = policyTemplateFactory.createArtifact(application, policyTemplateDescriptor);
        registeredPolicyTemplate = of(new RegisteredPolicyTemplate(policyTemplate));
        registeredPolicyTemplates.add(registeredPolicyTemplate.get());
      }

      ApplicationPolicyInstance applicationPolicyInstance = policyInstanceProviderFactory
          .create(application, registeredPolicyTemplate.get().policyTemplate, parametrization);

      applicationPolicyInstance.initialise();

      registeredPolicyInstanceProviders
          .add(new RegisteredPolicyInstanceProvider(applicationPolicyInstance, parametrization.getId()));
      registeredPolicyInstanceProviders.sort(null);
      registeredPolicyTemplate.get().count++;
    } catch (Exception e) {
      throw new PolicyRegistrationException(createPolicyRegistrationError(parametrization.getId()), e);
    }
  }

  @Override
  public synchronized boolean removePolicy(String parametrizedPolicyId) {
    Optional<RegisteredPolicyInstanceProvider> registeredPolicyInstanceProvider = registeredPolicyInstanceProviders.stream()
        .filter(p -> p.getPolicyId().equals(parametrizedPolicyId)).findFirst();

    registeredPolicyInstanceProvider.ifPresent(provider -> {
      provider.getApplicationPolicyInstance().dispose();
      registeredPolicyInstanceProviders.remove(provider);

      Optional<RegisteredPolicyTemplate> registeredPolicyTemplate = registeredPolicyTemplates.stream()
          .filter(p -> p.policyTemplate.equals(provider.getApplicationPolicyInstance().getPolicyTemplate()))
          .findFirst();

      if (!registeredPolicyTemplate.isPresent()) {
        throw new IllegalStateException("Cannot find registered policy template");
      }
      registeredPolicyTemplate.get().count--;
      if (registeredPolicyTemplate.get().count == 0) {
        application.getRegionClassLoader()
            .removeClassLoader(registeredPolicyTemplate.get().policyTemplate.getArtifactClassLoader());
        registeredPolicyTemplate.get().policyTemplate.dispose();
        registeredPolicyTemplates.remove(registeredPolicyTemplate.get());
      }
    });

    return registeredPolicyInstanceProvider.isPresent();
  }

  @Override
  public List<Policy> findSourceParameterizedPolicies(PolicyPointcutParameters policyPointcutParameters) {
    List<Policy> policies = new ArrayList<>();

    if (!registeredPolicyInstanceProviders.isEmpty()) {
      for (RegisteredPolicyInstanceProvider registeredPolicyInstanceProvider : registeredPolicyInstanceProviders) {
        if (registeredPolicyInstanceProvider.getApplicationPolicyInstance().getPointcut().matches(policyPointcutParameters)) {
          if (registeredPolicyInstanceProvider.getApplicationPolicyInstance().getSourcePolicy().isPresent()) {
            policies.add(registeredPolicyInstanceProvider.getApplicationPolicyInstance().getSourcePolicy().get());
          }
        }
      }
    }

    return policies;
  }

  @Override
  public List<Policy> findOperationParameterizedPolicies(PolicyPointcutParameters policyPointcutParameters) {
    List<Policy> policies = new ArrayList<>();

    if (!registeredPolicyInstanceProviders.isEmpty()) {
      for (RegisteredPolicyInstanceProvider registeredPolicyInstanceProvider : registeredPolicyInstanceProviders) {
        if (registeredPolicyInstanceProvider.getApplicationPolicyInstance().getPointcut().matches(policyPointcutParameters)) {
          if (registeredPolicyInstanceProvider.getApplicationPolicyInstance().getOperationPolicy().isPresent()) {
            policies.add(registeredPolicyInstanceProvider.getApplicationPolicyInstance().getOperationPolicy().get());
          }
        }
      }
    }

    return policies;
  }

  @Override
  public void dispose() {

    for (RegisteredPolicyInstanceProvider registeredPolicyInstanceProvider : registeredPolicyInstanceProviders) {
      registeredPolicyInstanceProvider.getApplicationPolicyInstance().dispose();
    }
    registeredPolicyInstanceProviders.clear();

    for (RegisteredPolicyTemplate registeredPolicyTemplate : registeredPolicyTemplates) {
      try {
        registeredPolicyTemplate.policyTemplate.dispose();
      } catch (RuntimeException e) {
        // Ignore and continue
      }


      registeredPolicyTemplates.clear();
    }
  }

  public void setApplication(Application application) {
    this.application = application;
  }

  static String createPolicyAlreadyRegisteredError(String policyId) {
    return format("Policy already registered: '%s'", policyId);
  }

  static String createPolicyRegistrationError(String policyId) {
    return format("Error occured registering policy '%s'", policyId);
  }

  private static class RegisteredPolicyTemplate {

    private volatile int count;
    private final PolicyTemplate policyTemplate;

    private RegisteredPolicyTemplate(PolicyTemplate policyTemplate) {
      this.policyTemplate = policyTemplate;
    }
  }

  private static class RegisteredPolicyInstanceProvider implements Comparable<RegisteredPolicyInstanceProvider> {

    private final ApplicationPolicyInstance applicationPolicyInstance;
    private final String policyId;

    public RegisteredPolicyInstanceProvider(ApplicationPolicyInstance applicationPolicyInstance, String policyId) {
      this.applicationPolicyInstance = applicationPolicyInstance;
      this.policyId = policyId;
    }

    @Override
    public int compareTo(RegisteredPolicyInstanceProvider registeredPolicyInstanceProvider) {
      return compare(applicationPolicyInstance.getOrder(), registeredPolicyInstanceProvider.applicationPolicyInstance.getOrder());
    }

    public ApplicationPolicyInstance getApplicationPolicyInstance() {
      return applicationPolicyInstance;
    }

    public String getPolicyId() {
      return policyId;
    }
  }
}
