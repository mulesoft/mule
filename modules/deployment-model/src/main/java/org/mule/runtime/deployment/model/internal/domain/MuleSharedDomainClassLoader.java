/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.domain;

import static org.mule.runtime.container.api.MuleFoldersUtil.getDomainClassesFolder;
import static org.mule.runtime.deployment.model.internal.domain.DomainClassLoaderFactory.getDomainId;
import static org.mule.runtime.module.reboot.api.MuleContainerBootstrapUtils.getMuleConfDir;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Defines a {@link MuleArtifactClassLoader} for a domain artifact.
 */
public class MuleSharedDomainClassLoader extends MuleDeployableArtifactClassLoader implements ArtifactClassLoader {

  static {
    registerAsParallelCapable();
  }

  public MuleSharedDomainClassLoader(ArtifactDescriptor artifactDescriptor, ClassLoader parent,
                                     ClassLoaderLookupPolicy lookupPolicy, List<URL> urls,
                                     List<ArtifactClassLoader> artifactPluginClassLoaders) {
    super(getDomainId(artifactDescriptor.getName()), artifactDescriptor, urls.toArray(new URL[0]), parent, lookupPolicy,
          artifactPluginClassLoaders);
  }

  @Override
  public URL findResource(String name) {
    URL resource = super.findResource(name);
    if (resource == null) {
      File file = new File(getDomainClassesFolder(getArtifactDescriptor().getName()) + File.separator + name);
      if (file.exists()) {
        try {
          resource = file.toURI().toURL();
        } catch (MalformedURLException e) {
          logger.debug("Failure looking for resource", e);
        }
      }
    }
    return resource;
  }

  @Override
  protected String[] getLocalResourceLocations() {
    return new String[] {getDomainClassesFolder(getArtifactDescriptor().getName()).getAbsolutePath(),
        getMuleConfDir().getAbsolutePath()};
  }
}
