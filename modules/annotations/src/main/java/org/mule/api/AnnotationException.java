/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api;

import org.mule.config.i18n.Message;

/**
 * Root exception for errors related to annotation parsing
 */
public class AnnotationException extends MuleException
{
    public AnnotationException(Message message)
    {
        super(message);
    }

    public AnnotationException(Message message, Throwable cause)
    {
        super(message, cause);
    }
}
