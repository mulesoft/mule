package org.mule.module.launcher;

import org.mule.config.i18n.Message;

/**
 *
 */
public class DeploymentStopException extends DeploymentException
{

    public DeploymentStopException(Message message)
    {
        super(message);
    }

    public DeploymentStopException(Message message, Throwable cause)
    {
        super(message, cause);
    }
}
