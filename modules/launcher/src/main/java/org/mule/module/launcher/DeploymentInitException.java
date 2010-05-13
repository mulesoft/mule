package org.mule.module.launcher;

import org.mule.config.i18n.Message;

/**
 *
 */
public class DeploymentInitException extends DeploymentException
{

    public DeploymentInitException(Message message)
    {
        super(message);
    }

    public DeploymentInitException(Message message, Throwable cause)
    {
        super(message, cause);
    }
}
