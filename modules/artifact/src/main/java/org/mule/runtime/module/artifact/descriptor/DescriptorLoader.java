/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.descriptor;

import org.mule.runtime.core.config.bootstrap.ArtifactType;

import java.io.File;
import java.util.Map;

/**
 * Loads descriptors used to describe Mule artifacts
 *
 * @param <T> type of loaded objects
 */
public interface DescriptorLoader<T> {

  /**
   * @return the unique ID of the descriptor loader
   */
  String getId();

  /**
   * Loads a described object
   *
   * @param artifactFile {@link File} with the content of the artifact to work with. Non null
   * @param attributes collection of attributes describing the loader. Non null.
   * @return a {@link T} loaded with the given attributes from the artifact folder.
   * @throws InvalidDescriptorLoaderException when is not possible to load the object with the provided configuration.
   */
  T load(File artifactFile, Map<String, Object> attributes) throws InvalidDescriptorLoaderException;

  boolean supportsArtifactType(ArtifactType artifactType);

}
