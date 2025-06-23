/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.internal.descriptor;

import static org.mule.runtime.api.meta.MuleVersion.NO_REVISION;

import static java.lang.String.format;

import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidator;

/**
 * Validator for {@link ArtifactDescriptor} version.
 *
 * @since 4.1
 */
public class VersionFormatArtifactDescriptorValidator implements ArtifactDescriptorValidator {

  private boolean doNotFailIfBundleDescriptorNotPresent;

  /**
   * Creates an instance of this validator.
   *
   * @param doNotFailIfBundleDescriptorNotPresent true {@code false} allows no versions for an {@link ArtifactDescriptor}.
   */
  public VersionFormatArtifactDescriptorValidator(boolean doNotFailIfBundleDescriptorNotPresent) {
    this.doNotFailIfBundleDescriptorNotPresent = doNotFailIfBundleDescriptorNotPresent;
  }

  @Override
  public void validate(ArtifactDescriptor descriptor) {
    if (doNotFailIfBundleDescriptorNotPresent && descriptor.getBundleDescriptor() == null) {
      return;
    }
    doValidate(descriptor);
  }

  private void doValidate(ArtifactDescriptor descriptor) {
    String bundleDescriptorVersion = descriptor.getBundleDescriptor().getVersion();
    if (bundleDescriptorVersion == null) {
      throw new ArtifactDescriptorCreateException(format("No version specified in the bundle descriptor of the artifact %s",
                                                         descriptor.getName()));
    }
    MuleVersion artifactVersion = new MuleVersion(bundleDescriptorVersion);
    if (artifactVersion.getRevision() == NO_REVISION) {
      throw new ArtifactDescriptorCreateException(format("Artifact %s version %s must contain a revision number. The version format must be x.y.z and the z part is missing",
                                                         descriptor.getName(), artifactVersion));
    }
  }

}
