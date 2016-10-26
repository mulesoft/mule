/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.plugin.moved.loader;

import org.mule.runtime.deployment.model.api.plugin.moved.MalformedPluginException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Represents a plugin that was loaded from a zip file.
 *
 * @since 4.0
 */
public class ZipPluginDescriptorLoader extends AbstractPluginDescriptorLoader {

  @Override
  protected Optional<InputStream> get(File pluginLocation, String resource) throws MalformedPluginException {
    Optional<InputStream> inputStream = Optional.empty();

    ZipEntry descriptorEntry = getZipFile(pluginLocation).getEntry(resource);
    if (descriptorEntry != null) {
      try {
        inputStream = Optional.of(getZipFile(pluginLocation).getInputStream(descriptorEntry));
      } catch (IOException e) {
        //does nothing
      }
    }
    return inputStream;
  }

  public ZipFile getZipFile(File pluginLocation) throws MalformedPluginException {
    try {
      return new ZipFile(pluginLocation);
    } catch (IOException e) {
      throw new MalformedPluginException(String.format("Can't load zip file from [%s]", pluginLocation.getAbsolutePath()), e);
    }
  }
}
