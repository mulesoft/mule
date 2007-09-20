/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.boot;

import java.io.File;

public class GuiInstallerLibraryDownloader
{
    public static void main(String args[]) throws Exception
    {
        File muleHome = new File(args[0]);
        MuleBootstrapUtils.ProxyInfo proxyInfo = new MuleBootstrapUtils.ProxyInfo();
        if (args.length > 2)
        {
            proxyInfo.host = args[1];
            proxyInfo.port = args[2];
        }
        if (args.length > 4)
        {
            proxyInfo.username = args[3];
            proxyInfo.password = args[4];               
        }
        MuleBootstrapUtils.addLocalJarFilesToClasspath(muleHome, muleHome);
        MuleBootstrapUtils.addExternalJarFilesToClasspath(muleHome, proxyInfo);
    }
}
