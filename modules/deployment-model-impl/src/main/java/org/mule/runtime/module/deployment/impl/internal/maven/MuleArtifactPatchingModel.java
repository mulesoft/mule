/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.internal.util.JarUtils.loadFileContentFrom;
import static org.mule.runtime.module.deployment.impl.internal.maven.AbstractMavenClassLoaderModelLoader.MULE_ARTIFACT_PATCHES_JSON_LOCATION;
import static org.mule.runtime.module.deployment.impl.internal.maven.AbstractMavenClassLoaderModelLoader.MULE_ARTIFACT_PATCH_JSON_FILE_NAME;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.module.reboot.api.MuleContainerBootstrapUtils;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MuleArtifactPatchingModel {

  private static Map<String, MuleArtifactPatchingModel> loadedModelByJAr = new HashMap<>();

  private ArtifactCoordinates artifactCoordinates;
  private List<String> affectedVersions;

  static synchronized MuleArtifactPatchingModel loadModel(String jarFile) throws IOException {
    if (!loadedModelByJAr.containsKey(jarFile)) {
      File pluginPatchJarFile =
          new File(new File(MuleContainerBootstrapUtils.getMuleHome(), MULE_ARTIFACT_PATCHES_JSON_LOCATION), jarFile);
      Optional<byte[]> muleArtifactPatchContent =
          loadFileContentFrom(pluginPatchJarFile, MULE_ARTIFACT_PATCH_JSON_FILE_NAME);
      muleArtifactPatchContent.map(bytes -> deserialize(bytes))
          .orElseThrow(() -> new MuleRuntimeException(createStaticMessage(format("Invalid jar file %s. It does not contains descriptor %s",
                                                                                 jarFile, MULE_ARTIFACT_PATCH_JSON_FILE_NAME))));
    }
    return loadedModelByJAr.get(jarFile);
  }

  private static MuleArtifactPatchingModel deserialize(byte[] muleArtifactPatchBytes) {
    Gson gson = (new GsonBuilder()).enableComplexMapKeySerialization().setPrettyPrinting().create();
    MuleArtifactPatchingModel classLoaderModel =
        gson.fromJson(new String(muleArtifactPatchBytes), MuleArtifactPatchingModel.class);
    return classLoaderModel;
  }

  public ArtifactCoordinates getArtifactCoordinates() {
    return artifactCoordinates;
  }

  public void setArtifactCoordinates(ArtifactCoordinates artifactCoordinates) {
    this.artifactCoordinates = artifactCoordinates;
  }

  public List<String> getAffectedVersions() {
    return affectedVersions;
  }

  public void setAffectedVersions(List<String> affectedVersions) {
    this.affectedVersions = affectedVersions;
  }
}
