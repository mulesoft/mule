/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.plugin.moved.deployment;


import static java.lang.String.format;
import org.mule.runtime.deployment.model.api.plugin.moved.Plugin;
import org.mule.runtime.deployment.model.api.plugin.moved.PluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.moved.deployment.DeploymentModel;
import org.mule.runtime.deployment.model.api.plugin.moved.deployment.MalformedDeploymentModelException;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Given a {@link PluginDescriptor} this class will generate an {@link DeploymentModel} from it.
 *
 * //TODO MULE-10785 move this one to the DeploymentModel that already exist
 *
 * @since 1.0
 */
class FolderDeploymentModelLoader {

  public DeploymentModel load(Plugin plugin) throws MalformedDeploymentModelException {
    //TODO MULE-10785 code this one
    throw new RuntimeException("NOT YET IMPLEMENTED");
    //return new DefaultDeploymentModel(getUrls(getPluginLocation(plugin)));
  }

  private File getPluginLocation(Plugin plugin) throws MalformedDeploymentModelException {
    File pluginFolder;
    try {
      pluginFolder = new File(plugin.getLocation().toURI());
    } catch (URISyntaxException e) {
      throw new MalformedDeploymentModelException(format("Can't create file from plugin [%s], location [%s]",
                                                         plugin.getPluginDescriptor().getName(), plugin.getLocation()),
                                                  e);
    }
    return pluginFolder;
  }

  private URL[] getUrls(File pluginFolder) throws MalformedDeploymentModelException {
    List<File> files = new ArrayList<>();
    File classesFolder = new File(pluginFolder, "classes");
    if (classesFolder.exists() && classesFolder.isDirectory()) {
      files.add(classesFolder);
    }
    final File libDir = new File(pluginFolder, "lib");
    if (libDir.exists()) {
      files.addAll(Arrays.asList(libDir.listFiles(pathname -> pathname.getName().endsWith(".jar"))));
    }

    URL[] urls = new URL[files.size()];
    for (int i = 0; i < files.size(); i++) {
      try {
        urls[i] = files.get(i).toURI().toURL();
      } catch (MalformedURLException e) {
        throw new MalformedDeploymentModelException("Failed to create the URL",
                                                    e);
      }
    }
    return urls;
  }
}
