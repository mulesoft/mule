/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.artifact.descriptor.api;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.meta.model.ExtensionModel;

import java.util.Map;

/**
 * Generic descriptor that will be used to describe parameterization to construct {@link ExtensionModel}, {@link ClassLoader} and
 * any other descriptor that may arise in a future of {@link ArtifactPluginDescriptor}.
 * <p/>
 * Each {@link LoaderDescriber} will have an ID that will be used to discover any loader that's responsible of working with the
 * current set of attributes. It's up to each loader to validate the types, size and all that matters around the attributes.
 *
 * @since 4.5
 */
@NoImplement
public interface LoaderDescriber {

  /**
   * @return descriptor's ID that will be used to discover any object that matches with this ID.
   */
  String getId();

  /**
   * @return attributes that will be feed to the loader found through {@link #getId()}, where it's up to the loader's
   *         responsibilities to determine if the current structure of the values ({@link Object}) does match with the expected
   *         types.
   *         <p/>
   *         That implies each loader must evaluate on the attributes' values to be 100% sure it were formed correctly.
   */
  Map<String, Object> getAttributes();

  /**
   * Stores all the entries of {@code attributes} under in the {@code attributes} internal state.
   *
   * @param attributes mappings to be stored in this map
   */
  void addAttributes(Map<String, Object> attributes);

}
