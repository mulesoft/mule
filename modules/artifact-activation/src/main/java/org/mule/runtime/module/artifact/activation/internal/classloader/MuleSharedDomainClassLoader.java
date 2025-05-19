/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.classloader;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.container.api.MuleFoldersUtil.getDomainFolder;
import static org.mule.runtime.module.artifact.api.classloader.BlockingLoggerResolutionClassRegistry.getBlockingLoggerResolutionClassRegistry;

import static java.io.File.separator;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.module.artifact.activation.internal.nativelib.NativeLibraryFinder;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.slf4j.Logger;

/**
 * Defines a {@link MuleArtifactClassLoader} for a domain artifact.
 */
public class MuleSharedDomainClassLoader extends NativeLibraryLoaderMuleDeployableArtifactClassLoader {

  static {
    registerAsParallelCapable();
    getBlockingLoggerResolutionClassRegistry().registerClassNeedingBlockingLoggerResolution(MuleSharedDomainClassLoader.class);
  }

  private static final Logger LOGGER = getLogger(MuleSharedDomainClassLoader.class);

  private final NativeLibraryFinder nativeLibraryFinder;

  public MuleSharedDomainClassLoader(ArtifactDescriptor artifactDescriptor, ClassLoader parent,
                                     ClassLoaderLookupPolicy lookupPolicy, List<URL> urls) {
    this(artifactDescriptor, parent, lookupPolicy, urls, null);
  }

  public MuleSharedDomainClassLoader(ArtifactDescriptor artifactDescriptor, ClassLoader parent,
                                     ClassLoaderLookupPolicy lookupPolicy, List<URL> urls,
                                     NativeLibraryFinder nativeLibraryFinder) {
    super(getDomainId(artifactDescriptor.getName()), artifactDescriptor, parent, nativeLibraryFinder, urls, lookupPolicy);
    this.nativeLibraryFinder = nativeLibraryFinder;
  }

  @Override
  public String findLibrary(String name) {
    if (nativeLibraryFinder == null) {
      return super.findLibrary(name);
    }

    if (supportNativeLibraryDependencies) {
      loadNativeLibraryDependencies(name);
    }

    String libraryPath = super.findLibrary(name);

    libraryPath = nativeLibraryFinder.findLibrary(name, libraryPath);

    return libraryPath;
  }

  @Override
  public URL findResource(String name) {
    URL resource = super.findResource(name);
    if (resource == null) {
      File file = new File(getDomainFolder(getArtifactDescriptor().getName()) + separator + name);
      if (file.exists()) {
        try {
          resource = file.toURI().toURL();
        } catch (MalformedURLException e) {
          LOGGER.debug("Failure looking for resource", e);
        }
      }
    }
    return resource;
  }

  @Override
  protected String[] getLocalResourceLocations() {
    return new String[] {getDomainFolder(getArtifactDescriptor().getName()).getAbsolutePath()};
  }

  /**
   * @param domainName name of the domain. Non empty.
   * @return the unique identifier for the domain in the container.
   */
  public static String getDomainId(String domainName) {
    checkArgument(!isEmpty(domainName), "domainName cannot be empty");

    return "domain/" + domainName;
  }
}
