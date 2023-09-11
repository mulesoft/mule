/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.policy;

import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor.POLICY_EXPORTED_PACKAGES_ERROR;
import static org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor.POLICY_EXPORTED_RESOURCE_ERROR;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.ARTIFACT_DESCRIPTORS;

import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration.ClassLoaderConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Collections;

import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
@Story(ARTIFACT_DESCRIPTORS)
public class PolicyTemplateDescriptorTestCase extends AbstractMuleTestCase {

  private static final String POLICY_NAME = "policyName";

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void verifiesPolicyTemplateDoesNotExportPackages() throws Exception {
    PolicyTemplateDescriptor policyTemplateDescriptor = new PolicyTemplateDescriptor(POLICY_NAME);
    ClassLoaderConfiguration classLoaderConfiguration =
        new ClassLoaderConfigurationBuilder().exportingPackages(Collections.singleton("org.foo")).build();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(POLICY_EXPORTED_PACKAGES_ERROR);
    policyTemplateDescriptor.setClassLoaderConfiguration(classLoaderConfiguration);
  }

  @Test
  public void verifiesPolicyTemplateDoesNotExportResources() throws Exception {
    PolicyTemplateDescriptor policyTemplateDescriptor = new PolicyTemplateDescriptor(POLICY_NAME);
    ClassLoaderConfiguration classLoaderConfiguration =
        new ClassLoaderConfigurationBuilder().exportingResources(Collections.singleton("META-INF/foo.xml")).build();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(POLICY_EXPORTED_RESOURCE_ERROR);
    policyTemplateDescriptor.setClassLoaderConfiguration(classLoaderConfiguration);
  }
}
