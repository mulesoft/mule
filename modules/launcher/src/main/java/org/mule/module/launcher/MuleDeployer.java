package org.mule.module.launcher;

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
