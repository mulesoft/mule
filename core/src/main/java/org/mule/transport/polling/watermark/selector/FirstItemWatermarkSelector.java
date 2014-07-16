/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.polling.watermark.selector;

/**
 * Implementation of {@link WatermarkSelector} that selects the first value of the
 * set. Thread-safeness is not guaranteed. If your use case is concurrent, then you
 * need to synchronize access yourself.
 * 
 * @since 3.5.0
 */
public class FirstItemWatermarkSelector extends WatermarkSelector
{

    @Override
    public void acceptValue(Object value)
    {
        if (this.value == null)
        {
            this.value = value;
        }
    }
}
