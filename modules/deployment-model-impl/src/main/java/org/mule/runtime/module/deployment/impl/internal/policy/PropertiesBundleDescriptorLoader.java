/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorLoader;
import org.mule.runtime.module.artifact.api.descriptor.InvalidDescriptorLoaderException;

import java.io.File;
import java.util.Map;

/**
 * Loads a {@link BundleDescriptor} using properties defined in the stored descriptor loader.
 */
public class PropertiesBundleDescriptorLoader implements BundleDescriptorLoader {

  public static final String PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID = "PROPERTIES_EXTENSION";

  public static final String VERSION = "version";
  public static final String GROUP_ID = "groupId";
  public static final String ARTIFACT_ID = "artifactId";
  public static final String CLASSIFIER = "classifier";
  public static final String TYPE = "type";

  @Override
  public String getId() {
    return PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID;
  }

  /**
   * Loads a bundle descriptor from the provided properties
   *
   *
   * @param artifactFolder {@link File} where the current artifact to work with. Non null
   * @param attributes attributes defined in the loader.
   * @param artifactType the type of the artifact of the descriptor to be loaded.
   * @return a locator of the coordinates of the current artifact
   * @throws ArtifactDescriptorCreateException if any bundle descriptor required property is missing on the given attributes.
   */
  @Override
  public BundleDescriptor load(File artifactFolder, Map<String, Object> attributes, ArtifactType artifactType)
      throws InvalidDescriptorLoaderException {
    String version = (String) attributes.get(VERSION);
    String groupId = (String) attributes.get(GROUP_ID);
    String artifactId = (String) attributes.get(ARTIFACT_ID);
    String classifier = (String) attributes.get(CLASSIFIER);
    String type = (String) attributes.get(TYPE);
    try {
      return new BundleDescriptor.Builder().setVersion(version).setGroupId(groupId).setArtifactId(artifactId)
          .setClassifier(classifier).setType(type).build();
    } catch (IllegalArgumentException e) {
      throw new InvalidDescriptorLoaderException("Bundle descriptor attributes are not complete", e);
    }
  }
}
