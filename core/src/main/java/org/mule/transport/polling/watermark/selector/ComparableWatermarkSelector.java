/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.polling.watermark.selector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link WatermarkSelector}s that evaluate {@link Comparable}
 * objects. When a non {@link Comparable} value is received, is is discarded.
 * 
 * @since 3.5.0
 */
public abstract class ComparableWatermarkSelector extends WatermarkSelector
{

    private static final Logger logger = LoggerFactory.getLogger(ComparableWatermarkSelector.class);
    
    /**
     * Returns a boolean result depending on whether the current value should persist to the watermark.
     * 
     * If {@link MaxValueWatermarkSelector} is used, the method returns true if the first argument is larger
     * than the second argument. If {@link MinValueWatermarkSelector} is used, the method returns true if 
     * the first argument is smaller than the second argument.
     * 
     * If this method returns true, then the given value becomes the new watermark value
     * @param valueToCompare The object that should be compared to the current watermark value
     * @param watermarkValue The current watermark value.
     * @return Returns a boolean that indicates whether the value given in the first argument 
     * should be persisted to the watermark
     */
    protected abstract boolean compare(Comparable<Object> valueToCompare, Comparable<Object> watermarkValue);

    @Override
    public final void acceptValue(Object value)
    {
        if (value == null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(String.format("Received null value. Ignoring"));
            }
        }
        else if (value instanceof Comparable)
        {
            Comparable<Object> currentWatermark = (Comparable<Object>) this.value;
            Comparable<Object> possibleWatermark = (Comparable<Object>) value;
            
            if (currentWatermark == null || compare(possibleWatermark, currentWatermark)) {
            	this.value = value;
            }
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(String.format(
                    "This selector only accepts Comparable values but %s found instead. Ignoring.",
                    value.getClass().getCanonicalName()));
            }
        }
    }
}
