package org.mule.module.launcher;

import org.mule.config.i18n.Message;

/**
 *
 */
public class InstallException extends DeploymentException
{

    public InstallException(Message message)
    {
        super(message);
    }

    public InstallException(Message message, Throwable cause)
    {
        super(message, cause);
    }
}
