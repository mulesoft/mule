/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.test.internal;

import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.helloExtensionV1Plugin;
import static org.mule.runtime.module.deployment.test.internal.util.Utils.getResourceFile;
import static org.mule.tck.probe.PollingProber.DEFAULT_POLLING_INTERVAL;
import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.POLICY_DEPLOYMENT;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.mule.functional.api.flow.TestEventBuilder;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.policy.PolicyParametrization;
import org.mule.runtime.core.internal.policy.OperationPolicy;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.PolicyFileBuilder;
import org.mule.runtime.policy.api.PolicyPointcut;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.net.URISyntaxException;
import java.util.Collections;
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

  private static final long REF_ENQUEUED_TIMEOUT_MILLIS = 5000;
  private static final String APP_WITH_SIMPLE_EXTENSION_CONFIG = "app-with-simple-flow-config.xml";

  private static final String OS_POLICY_NAME = "object-store-policy";
  private static final String POLICY_WITH_OPERATION_NAME = "appPluginPolicy";
  private static final String POLICY_WITH_NON_BLOCKING_OPERATION_NAME = "policyWithNonBlockingOperation";

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
  @Description("There was an issue with the inner fluxes of a non-blocking operation in a policy being disposed (and never rebuilt) after another operation policy was undeployed/redeployed and the CompositeOperationPolicy sinks were completed")
  public void whenRedeployingOperationPolicyThenOtherOperationPoliciesWithNonBlockingOperationsStillWork() throws Exception {
    // We will be deploying two policies and redeploying one of them
    // It is important that the policy that remains deployed has a non-blocking operation in order to reproduce the issue
    PolicyFileBuilder policyToRedeployFileBuilder = createPolicyFileBuilder(POLICY_WITH_OPERATION_NAME);
    PolicyFileBuilder policyToKeepFileBuilder = createPolicyFileBuilder(POLICY_WITH_NON_BLOCKING_OPERATION_NAME);

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

    Reference<OperationPolicy> operationPolicyRef = getPrintMessageOperationPolicyRef(applicationFileBuilder.getId());

    policyManager.removePolicy(applicationFileBuilder.getId(), policyToRedeployFileBuilder.getArtifactId());

    // Waits for the disposal of the composite policy instance (not the policy artifact itself)
    waitEnqueued(operationPolicyRef);

    assertManualExecutionsCount(3);

    policyManager.addPolicy(applicationFileBuilder.getId(), policyToRedeployFileBuilder.getArtifactId(),
                            policyToRedeployParametrization);

    assertManualExecutionsCount(5);
  }

  private Reference<OperationPolicy> getPrintMessageOperationPolicyRef(String applicationId) {
    ArtifactContext applicationContext = findApp(applicationId, 1).getArtifactContext();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.buildFromStringRepresentation("hello:print-message");

    Component operation = applicationContext.getMuleContext().getConfigurationComponentLocator().find(componentIdentifier).get(0);
    Registry registry = applicationContext.getRegistry();
    FlowConstruct flowConstruct = (FlowConstruct) registry.lookupByName(operation.getLocation().getRootContainerName()).get();
    PolicyManager appPolicyManager = registry.lookupByType(PolicyManager.class).get();

    // Here we are relying on the fact that there is caching involved when creating an operation policy
    OperationPolicy operationPolicy = appPolicyManager.createOperationPolicy(operation,
                                                                             new TestEventBuilder().build(flowConstruct),
                                                                             Collections::emptyMap);
    OperationPolicy operationPolicy2 = appPolicyManager.createOperationPolicy(operation,
                                                                              new TestEventBuilder().build(flowConstruct),
                                                                              Collections::emptyMap);
    // Control test to make sure there is caching
    assertThat("Operation policies are not cached, so the test can give false positives",
               operationPolicy,
               is(sameInstance(operationPolicy2)));

    return new PhantomReference<>(operationPolicy, new ReferenceQueue<>());
  }

  private void waitEnqueued(Reference<?> reference) {
    probe(REF_ENQUEUED_TIMEOUT_MILLIS, DEFAULT_POLLING_INTERVAL, () -> {
      System.gc();
      assertThat(reference.isEnqueued(), is(true));
      return true;
    }, () -> "Expected reference to be enqueued already");
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


