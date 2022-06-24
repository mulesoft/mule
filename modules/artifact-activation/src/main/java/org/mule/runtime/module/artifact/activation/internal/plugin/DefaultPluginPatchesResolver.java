/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.plugin;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.container.api.MuleFoldersUtil.getMuleHomeFolder;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;

import static java.lang.String.format;
import static java.nio.file.Files.walk;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginPatchesResolver;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.slf4j.Logger;

/**
 * Default implementation of {@link PluginPatchesResolver}.
 *
 * @since 4.5
 */
public class DefaultPluginPatchesResolver implements PluginPatchesResolver {

  private static final Logger LOGGER = getLogger(DefaultPluginPatchesResolver.class);

  private static final String MULE_ARTIFACT_PATCHES_LOCATION = "lib/patches/mule-artifact-patches";
  private static final String PATCH_FILES_EXTENSION = "jar";

  @Override
  public List<URL> resolve(ArtifactCoordinates pluginArtifactCoordinates) {
    List<URL> patches = new ArrayList<>();

    String artifactId = pluginArtifactCoordinates.getGroupId() + ":"
        + pluginArtifactCoordinates.getArtifactId() + ":" + pluginArtifactCoordinates.getVersion();

    GenericVersionScheme genericVersionScheme = new GenericVersionScheme();
    Version pluginArtifactCoordinatesVersion;
    try {
      pluginArtifactCoordinatesVersion = genericVersionScheme.parseVersion(pluginArtifactCoordinates.getVersion());
    } catch (Exception e) {
      LOGGER.warn("Error parsing version '{}' for artifact '{}', patches against this artifact will not be applied",
                  pluginArtifactCoordinates.getVersion(), artifactId);

      return emptyList();
    }

    File muleArtifactPatchesFolder = new File(getMuleHomeFolder(), MULE_ARTIFACT_PATCHES_LOCATION);
    try {
      if (muleArtifactPatchesFolder.exists()) {
        patches = walk(muleArtifactPatchesFolder.toPath())
            .filter(patchFilePath -> getExtension(patchFilePath.toString()).endsWith(PATCH_FILES_EXTENSION))
            .map(patchFilePath -> {
              try {
                MuleArtifactPatchingModel muleArtifactPatchingModel = MuleArtifactPatchingModel.loadModel(patchFilePath.toFile());
                ArtifactCoordinates patchedArtifactCoordinates = muleArtifactPatchingModel.getArtifactCoordinates();

                if (patchedArtifactCoordinates.getGroupId().equals(pluginArtifactCoordinates.getGroupId()) &&
                    patchedArtifactCoordinates.getArtifactId().equals(pluginArtifactCoordinates.getArtifactId()) &&
                    patchedArtifactCoordinates.getClassifier().equals(MULE_PLUGIN_CLASSIFIER)) {
                  if (muleArtifactPatchingModel.getAffectedVersions()
                      .stream()
                      .anyMatch(affectedVersion -> {
                        try {
                          VersionConstraint versionConstraint = genericVersionScheme.parseVersionConstraint(affectedVersion);
                          return versionConstraint.containsVersion(pluginArtifactCoordinatesVersion);
                        } catch (InvalidVersionSpecificationException e) {
                          throw new MuleRuntimeException(createStaticMessage(format("Could not parse plugin patch affect version '%s'",
                                                                                    affectedVersion)),
                                                         e);
                        }
                      })) {
                    try {
                      LOGGER.info("Patching artifact '{}' with patch file '{}'", artifactId, patchFilePath);

                      return new File(getMuleHomeFolder(),
                                      patchFilePath.toString())
                                          .toURL();
                    } catch (MalformedURLException e) {
                      throw new MuleRuntimeException(e);
                    }
                  }
                }
              } catch (IOException e) {
                throw new MuleRuntimeException(createStaticMessage(format("There was an error processing the patch in '%s' file",
                                                                          patchFilePath)),
                                               e);
              }

              return null;
            })
            .filter(Objects::nonNull)
            .collect(toList());
      }
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage(format("There was an error processing the patches in '%s'",
                                                                muleArtifactPatchesFolder)),
                                     e);
    }

    return patches;
  }

}
