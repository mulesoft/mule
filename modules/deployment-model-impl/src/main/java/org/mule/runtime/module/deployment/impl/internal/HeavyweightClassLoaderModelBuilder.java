/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal;

import static com.vdurmont.semver4j.Semver.SemverType.LOOSE;

import org.mule.tools.api.classloader.model.Artifact;

import com.vdurmont.semver4j.Semver;

import java.io.File;


public class HeavyweightClassLoaderModelBuilder extends LightweightClassLoaderModelBuilder {

  private static final Semver CLASS_LOADER_MODEL_NEW_VERSION = new Semver("1.1.0", LOOSE);

  private org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel;

  public HeavyweightClassLoaderModelBuilder(org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel,
                                            File applicationFolder) {
    super(applicationFolder);
    this.packagerClassLoaderModel = packagerClassLoaderModel;
  }

  @Override
  protected void doExportSharedLibrariesResourcesAndPackages() {
    if (new Semver(packagerClassLoaderModel.getVersion(), LOOSE).isLowerThan(CLASS_LOADER_MODEL_NEW_VERSION)) {
      super.doExportSharedLibrariesResourcesAndPackages();
    } else {
      exportSharedLibrariesResourcesAndPackages();
    }
  }

  private void exportSharedLibrariesResourcesAndPackages() {
    packagerClassLoaderModel.getDependencies().stream()
        .filter(Artifact::isShared)
        .forEach(
                 sharedDep -> findAndExportSharedLibrary(
                                                         sharedDep.getArtifactCoordinates().getGroupId(),
                                                         sharedDep.getArtifactCoordinates().getArtifactId()));
  }
}
