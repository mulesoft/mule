/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.module.deployment.internal.TestArtifactsCatalog.helloExtensionV1Plugin;
import static org.mule.runtime.module.deployment.internal.util.Utils.getResourceFile;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.POLICY_DEPLOYMENT;

import static java.lang.String.format;
import static java.lang.Thread.sleep;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import org.mule.runtime.core.api.policy.PolicyParametrization;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.PolicyFileBuilder;
import org.mule.runtime.policy.api.PolicyPointcut;

import java.net.URISyntaxException;
import java.util.List;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import org.junit.Test;
import org.junit.runners.Parameterized;

/**
 * Contains test for policy redeployment
 */
@Feature(POLICY_DEPLOYMENT)
public class ApplicationPolicyRedeploymentTestCase extends AbstractDeploymentTestCase {

  private static final String APP_WITH_SIMPLE_EXTENSION_CONFIG = "app-with-simple-flow-config.xml";

  private static final String OS_POLICY_NAME = "object-store-policy";
  private static final String POLICY_WITH_OPERATION_NAME = "appPluginPolicy";
  private static final String POLICY_WITH_ASYNC_OPERATION_NAME = "policyWithAsyncOperation";

  private static final String FLOW_NAME = "main";

  private static final PolicyPointcut ALWAYS_APPLIED_POLICY_POINTCUT = parameters -> true;

  @Parameterized.Parameters(name = "Parallel: {0}")
  public static List<Boolean> params() {
    // Only run without parallel deployment since this configuration does not affect policy deployment at all
    return singletonList(false);
  }

  public ApplicationPolicyRedeploymentTestCase(boolean parallelDeployment) {
    super(parallelDeployment);
  }

  @Test
  public void objectStoreSamePartitionWorksAfterRedeployingPolicy() throws Exception {
    PolicyFileBuilder policy = createPolicyFileBuilder(OS_POLICY_NAME, singletonMap("store", ""));
    PolicyParametrization policyParametrization = getParametrization(policy, 1);
    policyManager.registerPolicyTemplate(policy.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder =
        createExtensionApplicationWithServices(APP_WITH_SIMPLE_EXTENSION_CONFIG, helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), policy.getArtifactId(), policyParametrization);

    executeApplicationFlow(FLOW_NAME);

    policyManager.removePolicy(applicationFileBuilder.getId(), policy.getArtifactId());

    executeApplicationFlow(FLOW_NAME);

    policyManager.addPolicy(applicationFileBuilder.getId(), policy.getArtifactId(), policyParametrization);

    executeApplicationFlow(FLOW_NAME);
  }

  @Test
  @Issue("W-13563214")
  @Description("There was an issue with the inner fluxes of an async operation in a policy being disposed (and never rebuilt) after another operation policy was undeployed/redeployed and the CompositeOperationPolicy sinks were completed")
  public void whenRedeployingOperationPolicyThenOtherOperationPoliciesWithAsyncOperationsStillWork() throws Exception {
    // We will be deploying two policies and redeploying one of them
    // It is important that the policy that remains deployed has an asynchronous operation in order to reproduce the issue
    PolicyFileBuilder policyToRedeployFileBuilder = createPolicyFileBuilder(POLICY_WITH_OPERATION_NAME);
    PolicyFileBuilder policyToKeepFileBuilder = createPolicyFileBuilder(POLICY_WITH_ASYNC_OPERATION_NAME);

    PolicyParametrization policyToRedeployParametrization = getParametrization(policyToRedeployFileBuilder, 1);
    PolicyParametrization policyToKeepParametrization = getParametrization(policyToKeepFileBuilder, 2);

    policyManager.registerPolicyTemplate(policyToRedeployFileBuilder.getArtifactFile());
    policyManager.registerPolicyTemplate(policyToKeepFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), policyToRedeployFileBuilder.getArtifactId(),
                            policyToRedeployParametrization);
    policyManager.addPolicy(applicationFileBuilder.getId(), policyToKeepFileBuilder.getArtifactId(), policyToKeepParametrization);

    assertManualExecutionsCount(2);

    policyManager.removePolicy(applicationFileBuilder.getId(), policyToRedeployFileBuilder.getArtifactId());

    // TODO: actually wait until the stale policy is disposed to avoid a source of test flakiness
    sleep(2000);

    assertManualExecutionsCount(3);

    policyManager.addPolicy(applicationFileBuilder.getId(), policyToRedeployFileBuilder.getArtifactId(),
                            policyToRedeployParametrization);

    assertManualExecutionsCount(5);
  }

  private PolicyParametrization getParametrization(PolicyFileBuilder policyFileBuilder, int order) throws URISyntaxException {
    // Notice that the policy's artifactId needs to match with the resource file name for this to work
    return new PolicyParametrization(policyFileBuilder.getArtifactId(),
                                     ALWAYS_APPLIED_POLICY_POINTCUT,
                                     order,
                                     emptyMap(),
                                     getResourceFile(format("/%s.xml", policyFileBuilder.getArtifactId())),
                                     emptyList());
  }
}


