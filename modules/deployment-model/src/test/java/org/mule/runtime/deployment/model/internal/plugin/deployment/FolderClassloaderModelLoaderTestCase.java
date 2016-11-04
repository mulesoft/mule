/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.plugin.deployment;

import org.mule.runtime.deployment.model.api.plugin.loader.MalformedPluginException;
import org.mule.runtime.deployment.model.api.plugin.loader.Plugin;

import java.io.File;

public class FolderClassloaderModelLoaderTestCase extends AbstractClassloaderModelLoaderTestCase {

  @Override
  protected Plugin getPlugin(String parentFolder, String pluginFolder) throws MalformedPluginException {
    String folderPath =
        getClass().getClassLoader().getResource(new File(parentFolder, pluginFolder).getPath()).getFile();
    File file = new File(folderPath);
    return Plugin.from(file);
  }

  @Override
  protected String getRuntimeClasses() {
    return "classes";
  }

}
