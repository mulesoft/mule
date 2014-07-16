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

    private Controller controller;

    private static final int DEFAULT_TIMEOUT = 60000;

    public MuleProcessController(String muleHome)
    {
        this(muleHome, DEFAULT_TIMEOUT);
    }

    public MuleProcessController(String muleHome, int timeout)
    {
        this.controller = SystemUtils.IS_OS_WINDOWS ? new WindowsController(muleHome, timeout) : new UnixController(muleHome, timeout);
    }

    public boolean isRunning()
    {
        return this.controller.isRunning();
    }

    public void start(String... args)
    {
        this.controller.start(args);
    }

    public void stop(String... args)
    {
        this.controller.stop(args);
    }

    public int status(String... args)
    {
        return this.controller.status(args);
    }

    public int getProcessId()
    {
        return this.controller.getProcessId();
    }

    public void restart(String... args)
    {
        this.controller.restart(args);
    }



    public void deploy(String path)
    {
        this.controller.deploy(path);
    }

    public boolean isDeployed(String appName)
    {
        return this.controller.isDeployed(appName);
    }



    public void undeployAll()
    {
        this.controller.undeployAll();
    }

    public void installLicense(String path)
    {
        this.controller.installLicense(path);
    }

    public void uninstallLicense()
    {
        this.controller.uninstallLicense();
    }

    public void addLibrary(File jar)
    {
        this.controller.addLibrary(jar);
    }

    public void deployDomain(String domain)
    {
        this.controller.deployDomain(domain);
    }




}
