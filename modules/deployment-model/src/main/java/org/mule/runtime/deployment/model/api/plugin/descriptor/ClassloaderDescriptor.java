/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.plugin.descriptor;

import org.mule.runtime.deployment.model.api.plugin.classloadermodel.ClassloaderModel;
import org.mule.runtime.deployment.model.api.plugin.classloadermodel.MalformedClassloaderModelException;

import java.net.URL;
import java.util.Map;

/**
 * Marker interface to that generates a {@link ClassloaderModel} from a plugin's location and a set of attributes
 *
 * @since 4.0
 */
public interface ClassloaderDescriptor {

  /**
   * @return id of the current classloadermodel. Non null.
   */
  String getId();

  /**
   * Taking a location of a plugin and a set of attributes it generates the needed {@link ClassloaderModel}.
   *
   * @param location of the plugin
   * @param attributes map with attributes. It's up to each implementation of this method to validate if the set of
   *                   attributes is properly set, as this method is as generic as possible.
   * @return the {@link ClassloaderModel} with the values needed to create a classloader.
   * @throws MalformedClassloaderModelException if the attributes are malformed/wrong for the current classloadermodel.
     */
  ClassloaderModel load(URL location, Map<String, Object> attributes) throws MalformedClassloaderModelException;
}
