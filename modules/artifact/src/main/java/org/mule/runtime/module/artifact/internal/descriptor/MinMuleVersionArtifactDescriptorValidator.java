/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.internal.descriptor;

import static org.mule.runtime.manifest.api.MuleManifest.getMuleManifest;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidator;

import java.util.function.Supplier;

/**
 * Validator for {@link ArtifactDescriptor#getMinMuleVersion()}. By default compares versions using {@code major.minor.patch} but
 * can be configured to do the comparison using semantic version.
 *
 * @since 4.1
 */
public class MinMuleVersionArtifactDescriptorValidator implements ArtifactDescriptorValidator {

  private boolean validateMinMuleVersionWithSemanticVersion;
  private Supplier<String> muleRuntimeVersionSupplier = () -> getMuleManifest().getProductVersion();

  /**
   * Creates an instance of this validator.
   *
   * @param validateMinMuleVersionWithSemanticVersion {@code true} uses semantic version for checking
   *                                                  {@link ArtifactDescriptor#getMinMuleVersion()}.
   */
  public MinMuleVersionArtifactDescriptorValidator(boolean validateMinMuleVersionWithSemanticVersion) {
    this.validateMinMuleVersionWithSemanticVersion = validateMinMuleVersionWithSemanticVersion;
  }

  /**
   * Creates an instance of this validator.
   *
   * @param validateMinMuleVersionWithSemanticVersion {@code true} uses semantic version for checking
   *                                                  {@link ArtifactDescriptor#getMinMuleVersion()}.
   * @param muleRuntimeVersionSupplier                {@link Supplier} to get Mule Runtime version. Non null.
   */
  public MinMuleVersionArtifactDescriptorValidator(boolean validateMinMuleVersionWithSemanticVersion,
                                                   Supplier<String> muleRuntimeVersionSupplier) {
    this(validateMinMuleVersionWithSemanticVersion);
    requireNonNull(muleRuntimeVersionSupplier, "muleRuntimeVersionSupplier cannot be null");

    this.muleRuntimeVersionSupplier = muleRuntimeVersionSupplier;
  }

  @Override
  public void validate(ArtifactDescriptor descriptor) {
    MuleVersion minMuleVersion = descriptor.getMinMuleVersion();
    MuleVersion runtimeVersion = new MuleVersion(muleRuntimeVersionSupplier.get());
    runtimeVersion = new MuleVersion(runtimeVersion.toCompleteNumericVersion().replace("-" + runtimeVersion.getSuffix(), ""));
    if (validateMinMuleVersionWithSemanticVersion) {
      minMuleVersion = toBaseVersion(minMuleVersion);
      runtimeVersion = toBaseVersion(runtimeVersion);
    }

    doValidation(descriptor, minMuleVersion, runtimeVersion);
  }

  private MuleVersion toBaseVersion(MuleVersion version) {
    return new MuleVersion(version.getMajor() + "." + version.getMinor());
  }

  private void doValidation(ArtifactDescriptor descriptor, MuleVersion minMuleVersion, MuleVersion runtimeVersion) {
    if (runtimeVersion.priorTo(minMuleVersion)) {
      throw new ArtifactDescriptorCreateException(format("Artifact %s requires a newest runtime version. Artifact required version is %s and Mule Runtime version is %s",
                                                         descriptor.getName(),
                                                         descriptor.getMinMuleVersion().toCompleteNumericVersion(),
                                                         runtimeVersion.toCompleteNumericVersion()));
    }
  }

}
