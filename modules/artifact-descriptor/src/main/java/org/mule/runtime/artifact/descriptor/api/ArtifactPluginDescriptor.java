/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.artifact.descriptor.api;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.deployment.meta.LicenseModel;
import org.mule.runtime.api.meta.model.ExtensionModel;

import java.util.Optional;

/**
 * Describes an artifact with classifier {@code mule-plugin}, used within an Application or Domain.
 * 
 * @since 4.9
 */
@NoImplement
public interface ArtifactPluginDescriptor extends ArtifactDescriptor {

  /**
   * @return the {@link LoaderDescriber} that will contain all mandatory values to generate an {@link ExtensionModel} from it.
   */
  Optional<? extends LoaderDescriber> getExtensionModelDescriptorProperty();

  // /**
  // * Takes a {@link LoaderDescriber} that should contain the values used to properly initialize an {@link ExtensionModel}
  // *
  // * @param extensionModelLoaderDescriber the {@link LoaderDescriber} with the values
  // */
  // void setExtensionModelDescriptorProperty(LoaderDescriber extensionModelLoaderDescriber);

  /**
   * @return the license requirements for this plugin. Empty if there are not license requirements.
   */
  Optional<LicenseModel> getLicenseModel();

  // /**
  // * @param licenseModel the license requirements for this plugin.
  // */
  // void setLicenseModel(LicenseModel licenseModel);

}
