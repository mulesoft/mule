/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.api.policy;

import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor.POLICY_EXPORTED_PACKAGES_ERROR;
import static org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor.POLICY_EXPORTED_RESOURCE_ERROR;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class PolicyTemplateDescriptorTestCase extends AbstractMuleTestCase {

  private static final String POLICY_NAME = "policyName";

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void verifiesPolicyTemplateDoesNotExportPackages() throws Exception {
    PolicyTemplateDescriptor policyTemplateDescriptor = new PolicyTemplateDescriptor(POLICY_NAME);
    ClassLoaderModel classLoaderModel = mock(ClassLoaderModel.class);
    when(classLoaderModel.getExportedPackages()).thenReturn(Collections.singleton("org.foo"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(POLICY_EXPORTED_PACKAGES_ERROR);
    policyTemplateDescriptor.setClassLoaderModel(classLoaderModel);
  }

  @Test
  public void verifiesPolicyTemplateDoesNotExportResources() throws Exception {
    PolicyTemplateDescriptor policyTemplateDescriptor = new PolicyTemplateDescriptor(POLICY_NAME);
    ClassLoaderModel classLoaderModel = mock(ClassLoaderModel.class);
    when(classLoaderModel.getExportedResources()).thenReturn(Collections.singleton("META-INF/foo.xml"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(POLICY_EXPORTED_RESOURCE_ERROR);
    policyTemplateDescriptor.setClassLoaderModel(classLoaderModel);
  }
}
