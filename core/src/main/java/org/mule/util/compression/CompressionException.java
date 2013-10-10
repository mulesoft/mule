/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.compression;

import java.io.IOException;

/**
 * <code>CompressionException</code> TODO document
 */
public class CompressionException extends IOException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 8587532237749889185L;

    public CompressionException(String message)
    {
        super(message);
    }

    public CompressionException(String message, Throwable cause)
    {
        super(message);
        initCause(cause);
    }

}
