/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp.exception;

import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.Message;

/**
 * <code>MissingSecretPGPKeyException</code> is the exception in case data
 * regarding the pgp keys is necessary but no provided in the configuration.
 */
public class MissingPGPKeyException extends MuleRuntimeException
{

    private static final long serialVersionUID = -9046743821025012843L;

    /**
     * @param message message for the exception
     */
    public MissingPGPKeyException(Message message)
    {
        super(message);
    }

    public MissingPGPKeyException(Throwable cause)
    {
        super(cause);
    }
}
