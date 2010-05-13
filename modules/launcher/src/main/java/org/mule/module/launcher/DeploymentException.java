package org.mule.module.launcher;

import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.Message;

/**
 *
 */
public class DeploymentException extends MuleRuntimeException
{

    public DeploymentException(Message message)
    {
        super(message);
    }

    public DeploymentException(Message message, Throwable cause)
    {
        super(message, cause);
    }
}
