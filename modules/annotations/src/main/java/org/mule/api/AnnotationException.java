/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
