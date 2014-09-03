/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.infrastructure.process;

import java.io.File;

import org.apache.commons.lang.SystemUtils;

public class MuleProcessController
{

    private static final int DEFAULT_TIMEOUT = 60000;
    private Controller controller;

    public MuleProcessController(String muleHome)
    {
        this(muleHome, DEFAULT_TIMEOUT);
    }

    public MuleProcessController(String muleHome, int timeout)
    {
        controller = SystemUtils.IS_OS_WINDOWS ? new WindowsController(muleHome, timeout) : new UnixController(muleHome, timeout);
    }

    public boolean isRunning()
    {
        return controller.isRunning();
    }

    public void start(String... args)
    {
        controller.start(args);
    }

    public void stop(String... args)
    {
        controller.stop(args);
    }

    public int status(String... args)
    {
        return controller.status(args);
    }

    public int getProcessId()
    {
        return controller.getProcessId();
    }

    public void restart(String... args)
    {
        controller.restart(args);
    }



    public void deploy(String path)
    {
        controller.deploy(path);
    }

    public boolean isDeployed(String appName)
    {
        return controller.isDeployed(appName);
    }


    public void undeployAll()
    {
        controller.undeployAll();
    }

    public void installLicense(String path)
    {
        controller.installLicense(path);
    }

    public void uninstallLicense()
    {
        controller.uninstallLicense();
    }

    public void addLibrary(File jar)
    {
        controller.addLibrary(jar);
    }

    public void deployDomain(String domain)
    {
        controller.deployDomain(domain);
    }

    public File getLog()
    {
        return controller.getLog();
    }

    public File getLog(String appName)
    {
        return controller.getLog(appName);
    }

}
