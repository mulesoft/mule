/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.plugin;


import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor;

import java.util.Optional;

public class ArtifactPluginDescriptor extends DeployableArtifactDescriptor {

  private static final String META_INF = "META-INF";
  public static final String MULE_PLUGIN_CLASSIFIER = "mule-plugin";
  public static final String EXTENSION_BUNDLE_TYPE = "jar";
  private static final String MULE_ARTIFACT = "mule-artifact";

  /**
   * Target path as URL for any files used at deployment time or when generating the {@link ExtensionModel}
   */
  public static final String MULE_ARTIFACT_PATH_INSIDE_JAR = META_INF + "/" + MULE_ARTIFACT;
  public static final String MULE_AUTO_GENERATED_ARTIFACT_PATH_INSIDE_JAR =
      META_INF + "/auto-generated-" + MULE_ARTIFACT_JSON_DESCRIPTOR;;
  public static final String MULE_PLUGIN_POM = "pom.xml";

  private Optional<LoaderDescriber> extensionModelDescriptorProperty = empty();

  /**
   * Creates a new artifact plugin descriptor
   *
   * @param name artifact plugin name. Non empty.
   */
  public ArtifactPluginDescriptor(String name) {
    super(name);
  }

  /**
   * @return the {@link LoaderDescriber} that will contain all mandatory values to generate an {@link ExtensionModel} from it.
   */
  public Optional<LoaderDescriber> getExtensionModelDescriptorProperty() {
    return extensionModelDescriptorProperty;
  }

  /**
   * Takes a {@link LoaderDescriber} that should contain the values used to properly initialize an {@link ExtensionModel}
   *
   * @param extensionModelLoaderDescriber the {@link LoaderDescriber} with the values
   */
  public void setExtensionModelDescriptorProperty(LoaderDescriber extensionModelLoaderDescriber) {
    this.extensionModelDescriptorProperty = ofNullable(extensionModelLoaderDescriber);
  }
}
