/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import static org.mule.runtime.module.launcher.MuleFoldersUtil.getDomainFolder;

import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.reboot.MuleContainerBootstrapUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Load $MULE_HOME/lib/shared/<domain> libraries.
 */
public class MuleSharedDomainClassLoader extends MuleArtifactClassLoader implements ArtifactClassLoader {

  static {
    registerAsParallelCapable();
  }

  public MuleSharedDomainClassLoader(String domain, ClassLoader parent, ClassLoaderLookupPolicy lookupPolicy, List<URL> urls) {
    super(domain, urls.toArray(new URL[0]), parent, lookupPolicy);
  }

  @Override
  public URL findResource(String name) {
    URL resource = super.findResource(name);
    if (resource == null) {
      File file = new File(getDomainFolder(getArtifactName()) + File.separator + name);
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
    return new String[] {getDomainFolder(getArtifactName()).getAbsolutePath(),
        MuleContainerBootstrapUtils.getMuleConfDir().getAbsolutePath()};
  }
}
