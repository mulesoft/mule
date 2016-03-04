/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.descriptor;

import org.mule.MuleServer;
import org.mule.module.reboot.MuleContainerBootstrapUtils;

import java.io.File;

/**
 * Encapsulates defaults when no explicit descriptor provided with an app.
 */
public class EmptyApplicationDescriptor extends ApplicationDescriptor
{

    public EmptyApplicationDescriptor(String appName)
    {
        setName(appName);
        setConfigResources(new String[] {MuleServer.DEFAULT_CONFIGURATION});
        File configPathFile = MuleContainerBootstrapUtils.getMuleAppDefaultConfigFile(appName);
        String configPath = String.format(configPathFile.getAbsolutePath());
        setAbsoluteResourcePaths(new String[] {configPath});
        setConfigResourcesFile(new File[] {configPathFile});
    }
}
