/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.polling.watermark.selector;

/**
 * Implementation of {@link WatermarkSelector} that selectes the maximum value of the
 * set. For this to work, received values need to implement the {@link Comparable}
 * interface. Values that don't meet this condition will be discarded
 * 
 * @since 3.5.0
 */
public class MaxValueWatermarkSelector extends ComparableWatermarkSelector
{

    @Override
    protected int comparableQualifier()
    {
        return 1;
    }
}
