/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.descriptor;

import org.mule.api.annotation.NoImplement;

/**
 * {@link ArtifactDescriptorValidator} for {@link ArtifactDescriptor}.
 *
 * @since 4.1
 */
@NoImplement
public interface ArtifactDescriptorValidator {

  /**
   * Validates the {@link ArtifactDescriptor}. If validation fails a {@link org.mule.runtime.api.exception.MuleRuntimeException}
   * would be thrown.
   *
   * @param descriptor an {@link ArtifactDescriptor} to be validated.
   * @throws org.mule.runtime.api.exception.MuleRuntimeException
   */
  void validate(ArtifactDescriptor descriptor);

}
