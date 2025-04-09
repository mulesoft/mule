/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.test.internal.util;

import static org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor.EXTENSION_BUNDLE_TYPE;
import static org.mule.runtime.module.deployment.impl.internal.policy.loader.PropertiesBundleDescriptorLoader.ARTIFACT_ID;
import static org.mule.runtime.module.deployment.impl.internal.policy.loader.PropertiesBundleDescriptorLoader.CLASSIFIER;
import static org.mule.runtime.module.deployment.impl.internal.policy.loader.PropertiesBundleDescriptorLoader.GROUP_ID;
import static org.mule.runtime.module.deployment.impl.internal.policy.loader.PropertiesBundleDescriptorLoader.TYPE;
import static org.mule.runtime.module.deployment.impl.internal.policy.loader.PropertiesBundleDescriptorLoader.VERSION;

import static org.apache.commons.io.FileUtils.copyInputStreamToFile;

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.collection.SmallMap;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Utility class with methods for the deployment tests.
 */
public final class Utils {

  private Utils() {
    // Empty constructor to avoid accidental instantiations.
  }

  /**
   * Creates an instance of {@link File} with the path specified in the parameter {@code resource}.
   *
   * @param resource the path to the file.
   * @return a {@link File} representing the resource.
   * @throws URISyntaxException if an error occurred while trying to convert the URI.
   */
  public static File getResourceFile(String resource) throws URISyntaxException {
    return new File(Utils.class.getResource(resource).toURI());
  }

  /**
   * Same as {@link #getResourceFile(String)}, but creates the file as a child of the passed {@code tempFolder}.
   *
   * @param resource   path of the file to load.
   * @param tempFolder folder used as the parent.
   * @return a {@link File} representing the resource.
   */
  public static File getResourceFile(String resource, File tempFolder) {
    final File targetFile = new File(tempFolder, resource);
    try {
      copyInputStreamToFile(Utils.class.getResourceAsStream(resource), targetFile);
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
    return targetFile;
  }


  /**
   * Simplified way to obtain a {@link MuleArtifactLoaderDescriptor}.
   *
   * @see MuleArtifactLoaderDescriptor
   *
   * @param artifactId               value to be the "artifactId" attribute.
   * @param classifier               value to be the "classifier" attribute.
   * @param bundleDescriptorLoaderId id for the resulting descriptor.
   * @param version                  value to be the "version" attribute.
   * @return the descriptor.
   */
  public static MuleArtifactLoaderDescriptor createBundleDescriptorLoader(String artifactId, String classifier,
                                                                          String bundleDescriptorLoaderId, String version) {
    Map<String, Object> attributes = SmallMap.of(VERSION, version,
                                                 GROUP_ID, "org.mule.test",
                                                 ARTIFACT_ID, artifactId,
                                                 CLASSIFIER, classifier,
                                                 TYPE, EXTENSION_BUNDLE_TYPE);

    return new MuleArtifactLoaderDescriptor(bundleDescriptorLoaderId, attributes);
  }

  /**
   * @see #createBundleDescriptorLoader(String, String, String, String).
   */
  public static MuleArtifactLoaderDescriptor createBundleDescriptorLoader(String artifactId, String classifier,
                                                                          String bundleDescriptorLoaderId) {
    return createBundleDescriptorLoader(artifactId, classifier, bundleDescriptorLoaderId, "1.0.0");
  }
}
