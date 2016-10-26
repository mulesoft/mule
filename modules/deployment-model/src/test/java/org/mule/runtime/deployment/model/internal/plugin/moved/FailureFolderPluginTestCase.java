/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.plugin.moved;

import org.mule.runtime.deployment.model.api.plugin.moved.MalformedPluginException;
import org.mule.runtime.deployment.model.api.plugin.moved.Plugin;
import org.mule.runtime.deployment.model.api.plugin.moved.PluginDescriptor;

import java.io.File;

public class FailureFolderPluginTestCase extends FailureAbstractPluginTestCase {

  @Override
  protected PluginDescriptor getPluginDescriptor(String parentFolder, String pluginFolder) throws MalformedPluginException {
    String folderPath =
        getClass().getClassLoader().getResource(new File(parentFolder, pluginFolder).getPath()).getFile();

    File file = new File(folderPath);
    return Plugin.fromFolder(file).getPluginDescriptor();
  }
}
