/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
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
