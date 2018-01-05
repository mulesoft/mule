/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.descriptor;

import org.mule.runtime.module.artifact.internal.descriptor.CompositeArtifactDescriptorValidator;
import org.mule.runtime.module.artifact.internal.descriptor.MinMuleVersionArtifactDescriptorValidator;
import org.mule.runtime.module.artifact.internal.descriptor.MuleProductArtifactDescriptorValidator;
import org.mule.runtime.module.artifact.internal.descriptor.VersionFormatArtifactDescriptorValidator;

import com.google.common.collect.ImmutableList;

import java.util.function.Supplier;

/**
 * Builder that allows to create a {@link ArtifactDescriptorValidator} with different aspects:
 * from checking minMuleVersion to checking the artifact version format.
 *
 * @since 4.1
 */
public class ArtifactDescriptorValidatorBuilder {

  private boolean minMuleVersionArtifactDescriptorValidator;
  private boolean validateMinMuleVersionWithSemanticVersioning;
  private Supplier<String> muleRuntimeVersionSupplier;

  private boolean validateMuleProduct;

  private boolean validateVersionFormat;
  private boolean doNotFailIfBundleDescriptorNotPresent;

  private ArtifactDescriptorValidatorBuilder() {}

  /**
   * Creates an instance of the builder.
   *
   * @return a new builder instance.
   */
  public static ArtifactDescriptorValidatorBuilder builder() {
    return new ArtifactDescriptorValidatorBuilder();
  }

  /**
   * Sets a validation for minMuleVersion, {@link ArtifactDescriptor} created should defined the current Mule Runtime
   * version as minMuleVersion or a prior version.
   *
   * @return this
   */
  public ArtifactDescriptorValidatorBuilder validateMinMuleVersion() {
    this.minMuleVersionArtifactDescriptorValidator = true;
    return this;
  }

  /**
   * Sets a validation for minMuleVersion as {@link #validateMinMuleVersion()} but the Mule Runtime version could
   * be provided as a supplier. Used by Tooling.
   *
   * @return this
   */
  public ArtifactDescriptorValidatorBuilder validateMinMuleVersion(Supplier<String> muleRuntimeVersionSupplier) {
    this.validateMinMuleVersion();
    this.muleRuntimeVersionSupplier = muleRuntimeVersionSupplier;
    return this;
  }

  /**
   * Sets a validation for minMuleVersion as {@link #validateMinMuleVersion()} but the validation should only consider
   * semantic version when checking the minMuleVersion vs Mule Runtime version.
   *
   * @return this
   */
  public ArtifactDescriptorValidatorBuilder validateMinMuleVersionUsingSemanticVersion() {
    this.validateMinMuleVersion();
    this.validateMinMuleVersionWithSemanticVersioning = true;
    return this;
  }

  /**
   * Sets a validation for Mule product, {@link ArtifactDescriptor} should match {@link org.mule.runtime.api.deployment.meta.Product}.
   *
   * @return this
   */
  public ArtifactDescriptorValidatorBuilder validateMuleProduct() {
    this.validateMuleProduct = true;
    return this;
  }

  /**
   * Sets a validation for artifact version, {@link ArtifactDescriptor} should define its version in a particular format.
   * 
   * @return this
   */
  public ArtifactDescriptorValidatorBuilder validateVersionFormat() {
    this.validateVersionFormat = true;
    return this;
  }

  /**
   * Allows to avoid checking version format for {@link ArtifactDescriptor} that don't have a {@link BundleDescriptor}.
   *
   * @return this
   */
  public ArtifactDescriptorValidatorBuilder doNotFailIfBundleDescriptorNotPresentWhenValidationVersionFormat() {
    this.validateVersionFormat();
    this.doNotFailIfBundleDescriptorNotPresent = true;
    return this;
  }

  /**
   * @return a {@link ArtifactDescriptorValidator} with the constraints defined by this builder.
   */
  public ArtifactDescriptorValidator build() {
    ImmutableList.Builder<ArtifactDescriptorValidator> builder = ImmutableList.builder();
    if (minMuleVersionArtifactDescriptorValidator) {
      if (muleRuntimeVersionSupplier != null) {
        builder.add(new MinMuleVersionArtifactDescriptorValidator(validateMinMuleVersionWithSemanticVersioning,
                                                                  muleRuntimeVersionSupplier));
      } else {
        builder.add(new MinMuleVersionArtifactDescriptorValidator(validateMinMuleVersionWithSemanticVersioning));
      }
    }
    if (validateMuleProduct) {
      builder.add(new MuleProductArtifactDescriptorValidator());
    }
    if (validateVersionFormat) {
      builder.add(new VersionFormatArtifactDescriptorValidator(doNotFailIfBundleDescriptorNotPresent));
    }
    return new CompositeArtifactDescriptorValidator(builder.build());
  }

}
