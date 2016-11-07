/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.plugin;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.mule.module.artifact.classloader.net.MuleArtifactUrlConnection.SEPARATOR;
import static org.mule.module.artifact.classloader.net.MuleArtifactUrlStreamHandler.PROTOCOL;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Extracts the needed information of a Mule plugin when its current format is a ZIP file.
 *
 * @since 4.0
 */
public class ArtifactPluginDescriptorZipLoader extends AbstractArtifactPluginDescriptorLoader {

  public static final String EXTENSION_ZIP = ".zip";

  private final ZipFile pluginZip;

  /**
   * Stores the reference to a plugin so that it can constructs a {@link ZipFile} from it, to later generate a
   * {@link ArtifactPluginDescriptor} through the {@link #load()} method.
   * <p>
   * This class is particularly tight to the {@link org.mule.module.artifact.classloader.net.MuleArtifactUrlStreamHandler}
   * object, as it will rely on the {@link org.mule.module.artifact.classloader.net.MuleArtifactUrlStreamHandler#PROTOCOL}
   * to generate {@link URL}s that can be used later on the generation for future {@link ClassLoader}.
   *
   * @param pluginLocation location of a plugin
   * @see #assembleFor(String)
   */
  ArtifactPluginDescriptorZipLoader(File pluginLocation) {
    super(pluginLocation);
    try {
      this.pluginZip = new ZipFile(pluginLocation);
    } catch (IOException e) {
      throw new ArtifactDescriptorCreateException(format("There was an issue opening the ZIP file at '%s'",
                                                         pluginLocation.getAbsolutePath()),
                                                  e);
    }
  }

  @Override
  protected String getName() {
    return removeEnd(pluginLocation.getName(), EXTENSION_ZIP);
  }

  @Override
  protected URL getClassesUrl() throws MalformedURLException {
    return assembleFor(CLASSES);
  }

  @Override
  protected List<URL> getRuntimeLibs() throws MalformedURLException {
    List<URL> urls = new ArrayList<>();
    try {
      Enumeration<? extends ZipEntry> entries = pluginZip.entries();
      while (entries.hasMoreElements()) {
        String resourceEntry = entries.nextElement().getName();
        if (isRuntimeLib(resourceEntry)) {
          urls.add(assembleFor(resourceEntry));
        }
      }
    } catch (IOException e) {
      throw new ArtifactDescriptorCreateException(format("There was a problem while unzipping [%s]",
                                                         pluginLocation.toString()),
                                                  e);
    }
    return urls;
  }

  @Override
  protected Optional<InputStream> loadResourceFrom(String resource) {
    Optional<InputStream> inputStream;
    ZipEntry descriptorEntry = pluginZip.getEntry(resource);
    if (descriptorEntry != null) {
      try {
        inputStream = Optional.of(pluginZip.getInputStream(descriptorEntry));
      } catch (IOException e) {
        throw new ArtifactDescriptorCreateException(format("Cannot read resource '%s' from ZIP '%s'", resource,
                                                           pluginZip.getName()),
                                                    e);
      }
    } else {
      inputStream = empty();
    }
    return inputStream;
  }

  @Override
  protected void close() {
    try {
      pluginZip.close();
    } catch (IOException e) {
      throw new ArtifactDescriptorCreateException(format("There was a problem closing the ZIP file '%s'", pluginZip.getName()));
    }
  }

  /**
   * Taking a starting position, which is the the {@link File} pointing to the plugin, it generates an {@link URL} using
   * the {@link org.mule.module.artifact.classloader.net.MuleArtifactUrlStreamHandler#PROTOCOL} appending the desired
   * resource.
   * <p>
   * If the {@link #pluginLocation}'s {@link java.net.URI} is "file:/folderFoo/folderBar/mule-plugin.zip", the parametrized
   * {@code resource} was "classes" the output of this method will be an {@link URL} with the value of
   * "muleartifact:file:/folderFoo/folderBar/mule-plugin.zip!/classes!/".
   *
   * @param resource to append to the final {@link URL}
   * @return an {@link URL} using the {@link org.mule.module.artifact.classloader.net.MuleArtifactUrlStreamHandler#PROTOCOL}
   * @throws MalformedURLException if the protocol {@link org.mule.module.artifact.classloader.net.MuleArtifactUrlStreamHandler#PROTOCOL}
   * wasn't registered, or if the spec for the {@link URL} is malformed.
   */
  private URL assembleFor(String resource) throws MalformedURLException {
    return new URL(PROTOCOL + ":" + pluginLocation.toURI() + SEPARATOR + resource + SEPARATOR);
  }

  /**
   * @param resource to validate
   * @return true if the resource is a JAR file contained in {@link #LIB} folder, false otherwise.
   */
  private boolean isRuntimeLib(String resource) {
    return resource.startsWith(LIB + "/") && resource.endsWith(JAR_EXTENSION);
  }
}
