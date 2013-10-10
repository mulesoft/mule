/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.launcher;

import org.mule.module.launcher.application.Application;

import java.io.IOException;
import java.net.URL;

/**
 *
 */
public interface MuleDeployer
{

    void deploy(Application app);

    void undeploy(Application app);

    /**
     * Installs packaged Mule apps from $MULE_HOME/apps directory.
     * @param packedMuleAppFileName filename of the packed Mule app (only name + ext)
     */
    Application installFromAppDir(String packedMuleAppFileName) throws IOException;

    Application installFrom(URL url) throws IOException;
}
