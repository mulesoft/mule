/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher;

import static org.mule.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;

import java.io.File;

/**
 *  Calculates folders for a mule server based on the
 *  {@value org.mule.api.config.MuleProperties#MULE_HOME_DIRECTORY_PROPERTY} property
 */
public class MuleFoldersUtil
{

    public static final String EXECUTION_FOLDER = ".mule";
    public static final String LIB_FOLDER = "lib";
    public static final String APPS_FOLDER = "apps";
    public static final String PLUGINS_FOLDER = "plugins";

    private MuleFoldersUtil()
    {
    }

    public static File getMuleHomeFolder()
    {
        String muleHome = System.getProperty(MULE_HOME_DIRECTORY_PROPERTY, ".");

        return new File(muleHome);
    }

    public static File getAppsFolder()
    {
        return new File(getMuleHomeFolder(), APPS_FOLDER);
    }

    public static File getAppFolder(String appName)
    {
        return new File(getAppsFolder(), appName);
    }

    public static File getAppLibFolder(String appName)
    {
        return new File(getAppFolder(appName), LIB_FOLDER);
    }

    public static File getAppPluginsFolder(String appName)
    {
        return new File(getAppFolder(appName), PLUGINS_FOLDER);
    }

    public static File getAppSharedPluginLibsFolder(String appName)
    {
        return new File(getAppPluginsFolder(appName), LIB_FOLDER);
    }

    public static File getExecutionFolder()
    {
        return new File(getMuleHomeFolder(), EXECUTION_FOLDER);
    }

    public static File getMuleLibFolder()
    {
        return new File(getMuleHomeFolder(), LIB_FOLDER);
    }

    public static File getAppTempFolder(String appName)
    {
        return new File(getExecutionFolder(), appName);
    }
}
