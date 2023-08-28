/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.impl.internal.plugin;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.internal.util.jar.JarLoadingUtils.loadFileContentFrom;

import org.mule.runtime.api.exception.MuleRuntimeException;
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

  private static final String MULE_ARTIFACT_PATCH_JSON_FILE_NAME = "mule-artifact-patch.json";

  private static final Map<String, MuleArtifactPatchingModel> loadedModelByJar = new HashMap<>();

  private ArtifactCoordinates artifactCoordinates;
  private List<String> affectedVersions;

  public static synchronized MuleArtifactPatchingModel loadModel(File pluginPatchJarFile) throws IOException {
    final String key = pluginPatchJarFile.toString();
    if (!loadedModelByJar.containsKey(key)) {
      Optional<byte[]> muleArtifactPatchContent =
          loadFileContentFrom(pluginPatchJarFile, MULE_ARTIFACT_PATCH_JSON_FILE_NAME);
      muleArtifactPatchContent.map(bytes -> {
        MuleArtifactPatchingModel artifactPatchingModel = deserialize(bytes);
        loadedModelByJar.put(key, artifactPatchingModel);
        return artifactPatchingModel;
      })
          .orElseThrow(() -> new MuleRuntimeException(createStaticMessage(format("Invalid jar file '%s'. It does not contain descriptor '%s'",
                                                                                 pluginPatchJarFile,
                                                                                 MULE_ARTIFACT_PATCH_JSON_FILE_NAME))));
    }
    return loadedModelByJar.get(key);
  }

  private static MuleArtifactPatchingModel deserialize(byte[] muleArtifactPatchBytes) {
    Gson gson = (new GsonBuilder()).enableComplexMapKeySerialization().setPrettyPrinting().create();
    return gson.fromJson(new String(muleArtifactPatchBytes), MuleArtifactPatchingModel.class);
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

