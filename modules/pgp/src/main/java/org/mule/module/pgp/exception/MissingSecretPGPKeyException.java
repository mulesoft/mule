/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp.exception;

/**
 * <code>MissingSecretPGPKeyException</code> is the exception in case a decrypt
 * operation is attempted without a secret private key
 */
public class MissingSecretPGPKeyException extends Exception
{

    /**
     * 
     */
    private static final long serialVersionUID = -9046743821025012843L;

    /**
     * @param message message for the exception
     */
    public MissingSecretPGPKeyException(String message)
    {
        super(message);
    }
}
