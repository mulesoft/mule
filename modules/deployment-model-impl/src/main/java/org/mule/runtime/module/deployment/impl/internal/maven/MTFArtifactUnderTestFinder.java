/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.maven;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.EXTENSION_BUNDLE_TYPE;

import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleScope;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is responsible for finding an artifact and create a {@link BundleDescriptor} to be included in an application
 * that is going to be used to run MTF test cases.
 * <p>
 * It expects the classLoaderModel of the application provides the GAV of the module and an absolute path to were is
 * located.
 *
 * @since 4.2
 */
public class MTFArtifactUnderTestFinder {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  private static final String ARTIFACT_UNDER_TEST = "artifactUnderTest";
  private static final String ARTIFACT_GAV = "gav";
  private static final String ARTIFACT_URI = "path";
  private static final String ERROR = "Malformed GAV, expected [groupId:artifactId:classifier:version], but got [%s] elements";

  /**
   * Returns the {@link BundleDescriptor} of the tested artifact, if no `artifactUnderTest` attribute is defined then
   * returns an {@link Optional#empty()}.
   *
   * @param attributes the {@link ClassLoaderModel} additional attributes.
   * @return an {@link Optional} value with the {@link BundleDescriptor} of the tested artifact.
   */
  Optional<BundleDependency> find(Map<String, Object> attributes) {
    Map<String, String> artifact = ((Map<String, String>) attributes.getOrDefault(ARTIFACT_UNDER_TEST, emptyMap()));

    if (artifact.isEmpty()) {
      return empty();
    }

    String gav = artifact.get(ARTIFACT_GAV);
    logger.info("Including artifact [" + gav + "] to run MTF tests");

    return ofNullable(new BundleDependency.Builder()
        .setBundleUri(new File(artifact.get(ARTIFACT_URI)).toURI())
        .setScope(BundleScope.COMPILE)
        .setDescriptor(getBundleDescriptor(gav))
        .build());
  }

  private BundleDescriptor getBundleDescriptor(String gav) {
    String[] splittedGAV = gav.split(":");

    if (splittedGAV.length != 4) {
      throw new RuntimeException(String.format(ERROR, splittedGAV.length));
    }

    return new BundleDescriptor.Builder()
        .setGroupId(splittedGAV[0])
        .setArtifactId(splittedGAV[1])
        .setClassifier(splittedGAV[2])
        .setVersion(splittedGAV[3])
        .setType(EXTENSION_BUNDLE_TYPE)
        .build();
  }
}
