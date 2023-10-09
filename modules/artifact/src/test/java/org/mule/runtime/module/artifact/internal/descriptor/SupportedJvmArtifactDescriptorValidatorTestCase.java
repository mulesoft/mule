/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.internal.descriptor;

import static org.mule.runtime.module.artifact.internal.descriptor.SupportedJvmArtifactDescriptorValidator.JVM_ENFORCEMENT_LOOSE;
import static org.mule.runtime.module.artifact.internal.descriptor.SupportedJvmArtifactDescriptorValidator.JVM_ENFORCEMENT_STRICT;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.APP_DEPLOYMENT;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.DOMAIN_DEPLOYMENT;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.SupportedJavaVersions.ENFORCE_DEPLOYABLE_ARTIFACT_JAVA_VERSION;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;

import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.LinkedHashSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;

@Features({@Feature(APP_DEPLOYMENT), @Feature(DOMAIN_DEPLOYMENT)})
@Story(ENFORCE_DEPLOYABLE_ARTIFACT_JAVA_VERSION)
public class SupportedJvmArtifactDescriptorValidatorTestCase extends AbstractMuleTestCase {

  @Rule
  public final ExpectedException expected = none();

  @Test
  public void strict() {
    final DeployableArtifactDescriptor descriptor = mock(DeployableArtifactDescriptor.class);
    when(descriptor.getSupportedJavaVersions()).thenReturn(new LinkedHashSet<>(asList("1.8", "11", "17")));
    when(descriptor.getName()).thenReturn("myArtifact");

    new SupportedJvmArtifactDescriptorValidator("1.8", JVM_ENFORCEMENT_STRICT)
        .validate(descriptor);
    // no exception thrown
  }

  @Test
  public void strictFail() {
    final DeployableArtifactDescriptor descriptor = mock(DeployableArtifactDescriptor.class);
    when(descriptor.getSupportedJavaVersions()).thenReturn(new LinkedHashSet<>(asList("1.8", "11")));
    when(descriptor.getName()).thenReturn("myArtifact");

    final SupportedJvmArtifactDescriptorValidator validator =
        new SupportedJvmArtifactDescriptorValidator("17", JVM_ENFORCEMENT_STRICT);

    expected.expectMessage("Artifact 'myArtifact' does not support Java 17. Supported versions are: [1.8, 11]");
    validator.validate(descriptor);
  }

  @Test
  public void strictEmptyCollection() {
    final DeployableArtifactDescriptor descriptor = mock(DeployableArtifactDescriptor.class);
    when(descriptor.getSupportedJavaVersions()).thenReturn(emptySet());
    when(descriptor.getName()).thenReturn("myArtifact");

    new SupportedJvmArtifactDescriptorValidator("1.8", JVM_ENFORCEMENT_STRICT)
        .validate(descriptor);
    // no exception thrown
  }

  @Test
  public void loose() {
    final DeployableArtifactDescriptor descriptor = mock(DeployableArtifactDescriptor.class);
    when(descriptor.getSupportedJavaVersions()).thenReturn(new LinkedHashSet<>(asList("1.8", "11", "17")));
    when(descriptor.getName()).thenReturn("myArtifact");

    new SupportedJvmArtifactDescriptorValidator("1.8", JVM_ENFORCEMENT_LOOSE)
        .validate(descriptor);
    // no exception thrown
  }

  @Test
  public void looseFail() {
    final DeployableArtifactDescriptor descriptor = mock(DeployableArtifactDescriptor.class);
    when(descriptor.getSupportedJavaVersions()).thenReturn(new LinkedHashSet<>(asList("1.8", "11")));
    when(descriptor.getName()).thenReturn("myArtifact");

    final SupportedJvmArtifactDescriptorValidator validator =
        new SupportedJvmArtifactDescriptorValidator("17", JVM_ENFORCEMENT_LOOSE);

    validator.validate(descriptor);
    // no exception thrown
  }

}
