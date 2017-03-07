/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.builder;

import static org.mule.runtime.deployment.model.api.application.ApplicationDescriptor.REPOSITORY_FOLDER;
import org.mule.runtime.module.artifact.builder.AbstractArtifactFileBuilder;
import org.mule.runtime.module.artifact.builder.AbstractDependencyFileBuilder;
import org.mule.tck.ZipUtils;

import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public abstract class DeployableFileBuilder<T extends AbstractArtifactFileBuilder<T>> extends AbstractArtifactFileBuilder<T> {

  public DeployableFileBuilder(String id, boolean upperCaseInExtension) {
    super(id, upperCaseInExtension);
  }

  public DeployableFileBuilder(String id) {
    super(id);
  }

  public DeployableFileBuilder(T source) {
    super(source);
  }

  public DeployableFileBuilder(String id, T source) {
    super(id, source);
  }

  @Override
  protected final List<ZipUtils.ZipResource> getCustomResources() {
    final List<ZipUtils.ZipResource> customResources = new LinkedList<>();

    for (AbstractDependencyFileBuilder dependencyFileBuilder : getAllCompileDependencies()) {
      customResources.add(new ZipUtils.ZipResource(dependencyFileBuilder.getArtifactFile().getAbsolutePath(),
                                                   Paths.get(REPOSITORY_FOLDER,
                                                             dependencyFileBuilder.getArtifactFileRepositoryPath())
                                                       .toString()));
      customResources.add(new ZipUtils.ZipResource(dependencyFileBuilder.getArtifactPomFile().getAbsolutePath(),
                                                   Paths.get(REPOSITORY_FOLDER,
                                                             dependencyFileBuilder.getArtifactFilePomRepositoryPath())
                                                       .toString()));

    }

    customResources.addAll(doGetCustomResources());
    return customResources;
  }

  protected abstract List<ZipUtils.ZipResource> doGetCustomResources();


}
