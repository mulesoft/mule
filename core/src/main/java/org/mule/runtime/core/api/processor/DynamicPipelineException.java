/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.processor;

import org.mule.api.MuleException;
import org.mule.config.i18n.Message;

public class DynamicPipelineException extends MuleException
{

    public DynamicPipelineException(Message message, Throwable cause)
    {
        super(message, cause);
    }

    public DynamicPipelineException(Message message)
    {
        super(message);
    }

    public DynamicPipelineException(Throwable cause)
    {
        super(cause);
    }

}
