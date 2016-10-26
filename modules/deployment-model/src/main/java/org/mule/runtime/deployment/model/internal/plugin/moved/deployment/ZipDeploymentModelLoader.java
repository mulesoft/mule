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
import org.mule.runtime.module.artifact.net.MulePluginUrlStreamHandler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Given a {@link PluginDescriptor} this class will generate an {@link DeploymentModel} from it.
 *
 * //TODO MULE-10785 move this one to the DeploymentModel that already exist
 * @since 4.0
 */
class ZipDeploymentModelLoader {


  public DeploymentModel load(Plugin plugin) throws MalformedDeploymentModelException {
    try {
      boolean classesPresent = false;
      List<URL> urls = new ArrayList<>();
      ZipInputStream zipInputStream = new ZipInputStream(plugin.getLocation().openStream());
      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        if (entry.getName().startsWith("classes/")) {
          classesPresent = true;
        } else if (entry.getName().startsWith("lib/") && entry.getName().endsWith(".jar")) {
          urls.add(assembleFor(plugin, entry.getName()));
        }
      }
      if (classesPresent) {
        //need to be sure to add /classes at the very beginning
        urls.add(0, assembleFor(plugin, "classes"));
      }
      throw new RuntimeException("NOT YET IMPLEMENTED");
      //return new DefaultDeploymentModel(urls.toArray(new URL[urls.size()]));
    } catch (IOException e) {
      throw new MalformedDeploymentModelException(format("There was a problem while unziping [%s]",
                                                         plugin.getPluginDescriptor().getName()),
                                                  e);
    }
  }

  private URL assembleFor(Plugin plugin, String resource) throws MalformedURLException {
    return new URL(MulePluginUrlStreamHandler.PROTOCOL + ":" + plugin.getLocation() + "!/" + resource + "!/");
  }
}
