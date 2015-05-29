/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.functional;

import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.NonBlockingMessageSource;

/**
 * Use this class to provide a NonBlockingMessageSource to your test flows.
 */
public class TestNonBlockingSource implements NonBlockingMessageSource
{

    @Override
    public void setListener(MessageProcessor listener)
    {
        // Nothing to do here
    }
}
