package org.mule.module.launcher;

import org.mule.config.i18n.Message;

/**
 *
 */
public class DeploymentStartException extends DeploymentException
{

    public DeploymentStartException(Message message)
    {
        super(message);
    }

    public DeploymentStartException(Message message, Throwable cause)
    {
        super(message, cause);
    }
}
