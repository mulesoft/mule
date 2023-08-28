/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModelBuilder;
import org.mule.runtime.module.artifact.activation.internal.maven.LightweightDeployableProjectModelBuilder;

import java.io.File;
import java.util.Optional;

public abstract class AbstractDeployableProjectModelBuilder implements DeployableProjectModelBuilder {

  public static final String CLASSLOADER_MODEL_JSON_DESCRIPTOR = "classloader-model.json";
  public static final String CLASSLOADER_MODEL_JSON_PATCH_DESCRIPTOR = "classloader-model-patch.json";

  public static final String CLASSLOADER_MODEL_JSON_DESCRIPTOR_LOCATION =
      "META-INF/mule-artifact/" + CLASSLOADER_MODEL_JSON_DESCRIPTOR;
  public static final String CLASSLOADER_MODEL_JSON_PATCH_DESCRIPTOR_LOCATION =
      "META-INF/mule-artifact/" + CLASSLOADER_MODEL_JSON_PATCH_DESCRIPTOR;

  public static final String CLASS_LOADER_MODEL_VERSION_120 = "1.2.0";
  public static final String CLASS_LOADER_MODEL_VERSION_110 = "1.1.0";

  /**
   * Determines if the given project corresponds to a heavyweight package or a lightweight one.
   *
   * @param projectFolder the project package location.
   * @return {@code true} if the given project corresponds to a heavyweight package, {@code false} otherwise.
   */
  public static boolean isHeavyPackage(File projectFolder) {
    return getClassLoaderModelDescriptor(projectFolder).exists();
  }

  protected static File getClassLoaderModelDescriptor(File artifactFile) {
    if (artifactFile.isDirectory()) {
      return new File(artifactFile, CLASSLOADER_MODEL_JSON_DESCRIPTOR_LOCATION);
    } else {
      return new File(artifactFile.getParent(), CLASSLOADER_MODEL_JSON_DESCRIPTOR);
    }
  }

  public static AbstractDeployableProjectModelBuilder defaultDeployableProjectModelBuilder(File projectFolder,
                                                                                           Optional<MuleDeployableModel> model,
                                                                                           boolean isDomain) {
    if (isHeavyPackage(projectFolder)) {
      return new MuleDeployableProjectModelBuilder(projectFolder, model);
    } else {
      return new LightweightDeployableProjectModelBuilder(projectFolder, model, isDomain);
    }
  }

}
