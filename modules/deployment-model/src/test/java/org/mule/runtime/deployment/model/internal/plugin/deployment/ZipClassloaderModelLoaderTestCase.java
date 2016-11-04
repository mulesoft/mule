/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.plugin.deployment;

import org.mule.runtime.deployment.model.api.plugin.loader.MalformedPluginException;
import org.mule.runtime.deployment.model.api.plugin.loader.Plugin;
import org.mule.runtime.module.artifact.net.MulePluginUrlStreamHandler;
import org.mule.runtime.module.artifact.net.MuleUrlStreamHandlerFactory;
import org.mule.runtime.module.artifact.net.PluginZipUtils;

import java.io.File;
import java.net.MalformedURLException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class ZipClassloaderModelLoaderTestCase extends AbstractClassloaderModelLoaderTestCase {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Before
  public void setUp() throws MalformedURLException {
    MulePluginUrlStreamHandler.register();
    // register the custom UrlStreamHandlerFactory.
    MuleUrlStreamHandlerFactory.installUrlStreamHandlerFactory();
  }

  @Override
  protected Plugin getPlugin(String parentFolder, String pluginFolder) throws MalformedPluginException {
    String folderPath =
        getClass().getClassLoader().getResource(new File(parentFolder, pluginFolder).getPath()).getFile();
    File file = PluginZipUtils.zipDirectory(folderPath, folder, pluginFolder);
    return Plugin.from(file);
  }

  @Override
  protected String getRuntimeClasses() {
    return "classes!/";
  }

}
