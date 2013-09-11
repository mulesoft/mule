/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.processor;

import org.mule.devkit.processor.DevkitBasedMessageProcessor;

/**
 * This class is deprecated since 3.5.0 and will be removed in Mule 4.0. Please use
 * {@link org.mule.devkit.processor.DevkitBasedMessageProcessor} instead.
 */
@Deprecated
public abstract class AbstractDevkitBasedMessageProcessor extends DevkitBasedMessageProcessor
{

    public AbstractDevkitBasedMessageProcessor(String operationName)
    {
        super(operationName);
    }

}
