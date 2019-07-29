/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import static java.util.Collections.emptyList;
import static org.mule.runtime.module.deployment.impl.internal.maven.AbstractMavenClassLoaderModelLoader.MULE_ARTIFACT_PATCHES_JSON_FILE_NAME;
import static org.mule.runtime.module.deployment.impl.internal.maven.AbstractMavenClassLoaderModelLoader.MULE_ARTIFACT_PATCHES_JSON_LOCATION;
import org.mule.runtime.module.reboot.api.MuleContainerBootstrapUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

public class MuleArtifactPatchingModel {

  private static MuleArtifactPatchingModel loadedModel;

  private List<MuleArtifactPatchDescriptor> muleArtifactPatchDescriptors = emptyList();

  public List<MuleArtifactPatchDescriptor> getMuleArtifactPatchDescriptors() {
    return muleArtifactPatchDescriptors;
  }

  public void setMuleArtifactPatchDescriptors(List<MuleArtifactPatchDescriptor> muleArtifactPatchDescriptors) {
    this.muleArtifactPatchDescriptors = muleArtifactPatchDescriptors;
  }

  static synchronized MuleArtifactPatchingModel loadModel() {
    if (loadedModel == null) {
      File pluginPatchesDescriptor =
          new File(MuleContainerBootstrapUtils.getMuleHome(), MULE_ARTIFACT_PATCHES_JSON_LOCATION);
      loadedModel = new MuleArtifactPatchingModel();
      if (pluginPatchesDescriptor.exists()) {
        loadedModel = deserialize(pluginPatchesDescriptor, MULE_ARTIFACT_PATCHES_JSON_FILE_NAME);
      }
    }
    return loadedModel;
  }

  private static MuleArtifactPatchingModel deserialize(File classLoaderModelDescriptor, String fileName) {
    try {
      Gson gson = (new GsonBuilder()).enableComplexMapKeySerialization().setPrettyPrinting().create();
      Reader reader = new FileReader(classLoaderModelDescriptor);
      MuleArtifactPatchingModel classLoaderModel = gson.fromJson(reader, MuleArtifactPatchingModel.class);
      reader.close();
      return classLoaderModel;
    } catch (IOException e) {
      throw new RuntimeException("Could not create " + fileName, e);
    }
  }
}
