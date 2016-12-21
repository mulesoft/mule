/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.container.api.MuleFoldersUtil.getExecutionFolder;
import static org.mule.runtime.core.util.FileUtils.unzip;
import org.mule.runtime.core.policy.PolicyParametrization;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationPolicyManager;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateDescriptorFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Applies policies to each deployed application to simulate the real manager provided by API Gateway.
 */
public class TestPolicyManager implements DeploymentListener {

  private final DeploymentService deploymentService;
  private final Map<String, List<AppPolicyParametrization>> registeredPolicies = new HashMap<>();
  private final List<PolicyTemplateDescriptor> policyTemplateDescriptors = new ArrayList<>();
  private final PolicyTemplateDescriptorFactory policyTemplateDescriptorFactory = new PolicyTemplateDescriptorFactory();

  /**
   * Creates a new manager
   *
   * @param deploymentService service that deploys applications in the container. Non null.
   */
  public TestPolicyManager(DeploymentService deploymentService) {
    checkArgument(deploymentService != null, "deploymentService cannot be null");

    this.deploymentService = deploymentService;
  }

  @Override
  public void onDeploymentSuccess(String artifactName) {
    List<AppPolicyParametrization> appPolicyParametrizations = registeredPolicies.get(artifactName);

    if (appPolicyParametrizations == null) {
      return;
    }

    Application application = deploymentService.findApplication(artifactName);
    ApplicationPolicyManager policyManager = application.getPolicyManager();

    for (AppPolicyParametrization appPolicyParametrization : appPolicyParametrizations) {
      policyManager.addPolicy(appPolicyParametrization.policyTemplateDescriptor, appPolicyParametrization.policyParametrization);
    }
  }

  /**
   * Registers a policy template that can be later used to register a parameterized policy
   *
   * @param policyTemplate policy artifact file. Non null.
   */
  public void registerPolicyTemplate(File policyTemplate) {
    final File tempFolder = new File(getPoliciesTempFolder(), getBaseName(policyTemplate.getName()));
    try {
      unzip(policyTemplate, tempFolder);
    } catch (IOException e) {
      throw new IllegalStateException("Error processing policy ZIP file: " + policyTemplate, e);
    }

    final PolicyTemplateDescriptor policyTemplateDescriptor = policyTemplateDescriptorFactory.create(tempFolder);
    policyTemplateDescriptors.add(policyTemplateDescriptor);
  }

  /**
   * Adds a parameterized policy
   *
   * @param appName application where the policy must be applied. Non empty.
   * @param policyTemplateName template that must be used to instantiate the parametrized policy. Non empty.
   * @param policyParametrization parametrization to instantiate the policy. Non null.
   */
  public void addPolicy(String appName, String policyTemplateName, PolicyParametrization policyParametrization) {

    checkArgument(!isEmpty(appName), "appName cannot be empty");
    checkArgument(!isEmpty(policyTemplateName), "policyTemplateName cannot be empty");
    checkArgument(policyParametrization != null, "policyParametrization cannot be ull");

    Optional<PolicyTemplateDescriptor> policyTemplateDescriptor =
        policyTemplateDescriptors.stream().filter(template -> template.getName().equals(policyTemplateName)).findFirst();

    if (!policyTemplateDescriptor.isPresent()) {
      throw new IllegalStateException("Cannot find policy template descriptor with name: " + policyTemplateName);
    }

    List<AppPolicyParametrization> appPolicyParametrizations = registeredPolicies.get(appName);

    if (appPolicyParametrizations == null) {
      appPolicyParametrizations = new ArrayList<>();
      registeredPolicies.put(appName, appPolicyParametrizations);
    }

    appPolicyParametrizations.add(new AppPolicyParametrization(policyTemplateDescriptor.get(), policyParametrization));
  }

  private File getPoliciesTempFolder() {
    return new File(getExecutionFolder(), "policies");
  }

  private static class AppPolicyParametrization {

    private final PolicyTemplateDescriptor policyTemplateDescriptor;
    private final PolicyParametrization policyParametrization;

    private AppPolicyParametrization(PolicyTemplateDescriptor policyTemplateDescriptor,
                                     PolicyParametrization policyParametrization) {
      this.policyTemplateDescriptor = policyTemplateDescriptor;
      this.policyParametrization = policyParametrization;
    }
  }
}
