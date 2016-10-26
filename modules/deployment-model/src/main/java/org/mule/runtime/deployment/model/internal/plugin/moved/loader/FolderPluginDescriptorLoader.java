/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.plugin.moved.loader;

import org.mule.runtime.deployment.model.api.plugin.moved.MalformedPluginException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Represents a plugin that was loaded from a file.
 *
 * @since 4.0
 */
public class FolderPluginDescriptorLoader extends AbstractPluginDescriptorLoader {

  @Override
  protected Optional<InputStream> get(File pluginLocation, String resource) throws MalformedPluginException {
    Optional<InputStream> inputStream = Optional.empty();
    File file = new File(pluginLocation, resource);
    if (file.exists()) {
      try {
        inputStream = Optional.of(new FileInputStream(file));
      } catch (FileNotFoundException e) {
        //does nothing
      }
    }
    return inputStream;
  }
}
