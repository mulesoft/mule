/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.internal.descriptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorCreateException;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.jupiter.api.Test;

public class MinMuleVersionArtifactDescriptorValidatorTestCase extends AbstractMuleTestCase {

  @Test
  public void validationOk() {
    final var descriptor = mock(ArtifactDescriptor.class);
    when(descriptor.getMinMuleVersion()).thenReturn(new MuleVersion("4.6"));
    when(descriptor.getName()).thenReturn("test");

    final var validator = new MinMuleVersionArtifactDescriptorValidator(false, () -> "4.9");
    validator.validate(descriptor);
  }

  @Test
  public void validationFailure() {
    final var descriptor = mock(ArtifactDescriptor.class);
    when(descriptor.getMinMuleVersion()).thenReturn(new MuleVersion("4.99999"));
    when(descriptor.getName()).thenReturn("test");

    final var validator = new MinMuleVersionArtifactDescriptorValidator(false, () -> "4.9");
    var thrown = assertThrows(ArtifactDescriptorCreateException.class, () -> validator.validate(descriptor));
    assertThat(thrown.getMessage(),
               is("Artifact test requires a newest runtime version. Artifact required version is 4.99999.0 and Mule Runtime version is 4.9.0"));
  }

  @Test
  public void validateMinMuleVersionWithSemanticVersion() {
    final var descriptor = mock(ArtifactDescriptor.class);
    when(descriptor.getMinMuleVersion()).thenReturn(new MuleVersion("4.9.1"));
    when(descriptor.getName()).thenReturn("test");

    final var validator = new MinMuleVersionArtifactDescriptorValidator(true, () -> "4.9.0");
    validator.validate(descriptor);
  }

}
