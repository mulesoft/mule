/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.boot;

import java.io.File;

public class GuiInstallerLibraryDownloader
{
    public static void main(String args[]) throws Exception
    {
        File muleHome = new File(args[0]);
        MuleBootstrapUtils.addLocalJarFilesToClasspath(muleHome, muleHome);
    }
}
