/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MulePolicyModel.MulePolicyModelBuilder;
import org.mule.runtime.api.deployment.meta.Product;
import org.mule.runtime.core.api.policy.PolicyParametrization;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.PolicyFileBuilder;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Contains test for application deployment with policies on the default domain
 */
public class ApplicationPolicyRedeploymentTestCase extends AbstractDeploymentTestCase {

  private static final String APP_WITH_SIMPLE_EXTENSION_CONFIG = "app-with-simple-flow-config.xml";

  private static final String OS_POLICY_NAME = "object-store-policy";
  private static final String OS_POLICY_ID = "object-store-policy";
  public static final String FLOW_NAME = "main";

  public ApplicationPolicyRedeploymentTestCase(boolean parallelDeployment) {
    super(parallelDeployment);
  }

  @Test
  public void objectStoreSamePartitionWorksAfterRedeployingPolicy() throws Exception {
    policyManager.registerPolicyTemplate(policyWithPlugin().getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder =
        createExtensionApplicationWithServices(APP_WITH_SIMPLE_EXTENSION_CONFIG, helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    PolicyFileBuilder policy = policyWithPlugin();

    policyManager.addPolicy(applicationFileBuilder.getId(), policy.getArtifactId(),
                            new PolicyParametrization(OS_POLICY_ID, poinparameters -> true, 1, emptyMap(),
                                                      getResourceFile(format("/%s.xml", OS_POLICY_NAME)), emptyList()));

    executeApplicationFlow(FLOW_NAME);

    policyManager.removePolicy(applicationFileBuilder.getId(), policy.getArtifactId());

    executeApplicationFlow(FLOW_NAME);

    policyManager.addPolicy(applicationFileBuilder.getId(), policy.getArtifactId(),
                            new PolicyParametrization(OS_POLICY_ID, poinparameters -> true, 1, emptyMap(),
                                                      getResourceFile(format("/%s.xml", OS_POLICY_NAME)), emptyList()));

    executeApplicationFlow(FLOW_NAME);
  }

  private PolicyFileBuilder policyWithPlugin() {
    Map<String, Object> map = new HashMap<>();
    map.put("store", "");
    MulePolicyModelBuilder mulePolicyModelBuilder = new MulePolicyModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName(OS_POLICY_NAME)
        .setRequiredProduct(Product.MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader(OS_POLICY_NAME, MULE_POLICY_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID))
        .withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, map));

    return new PolicyFileBuilder(OS_POLICY_NAME)
        .describedBy(mulePolicyModelBuilder.build());
  }
}


