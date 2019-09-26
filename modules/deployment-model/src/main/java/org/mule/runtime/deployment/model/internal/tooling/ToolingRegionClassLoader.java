/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.tooling;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ResourceReleaser;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

/**
 * Tooling implementation of a {@link org.mule.runtime.module.artifact.api.classloader.RegionClassLoader} that allows
 * to override some behaviour.
 */
public class ToolingRegionClassLoader extends RegionClassLoader {

  private static final String CLASSLOADER_RELEASER_EXPLICIT_GC_SYSTEM_PROPERTY = SYSTEM_PROPERTY_PREFIX + "tooling.runExplicitGC";

  private static final boolean shouldRunGC;

  static {
    String shouldRunGCString = getProperty(CLASSLOADER_RELEASER_EXPLICIT_GC_SYSTEM_PROPERTY);
    if (shouldRunGCString != null) {
      shouldRunGC = parseBoolean(shouldRunGCString);
    } else {
      shouldRunGC = false;
    }
  }

  public static ToolingRegionClassLoader newToolingRegionClassLoader(String artifactId, ArtifactDescriptor artifactDescriptor,
                                                                     ClassLoader parent, ClassLoaderLookupPolicy lookupPolicy) {
    if (shouldRunGC) {
      return new ToolingRegionClassLoader(
                                          artifactId,
                                          artifactDescriptor,
                                          parent,
                                          lookupPolicy);
    }
    return new ToolingRegionClassLoader(
                                        artifactId,
                                        artifactDescriptor,
                                        parent,
                                        lookupPolicy, () -> {
                                        }); //No op resource releaser
  }

  private ToolingRegionClassLoader(String artifactId, ArtifactDescriptor artifactDescriptor, ClassLoader parent,
                                   ClassLoaderLookupPolicy lookupPolicy) {
    super(artifactId, artifactDescriptor, parent, lookupPolicy);
  }

  private ToolingRegionClassLoader(String artifactId, ArtifactDescriptor artifactDescriptor, ClassLoader parent,
                                   ClassLoaderLookupPolicy lookupPolicy, ResourceReleaser regionResourceReleaser) {
    super(artifactId, artifactDescriptor, parent, lookupPolicy, regionResourceReleaser);
  }
}
