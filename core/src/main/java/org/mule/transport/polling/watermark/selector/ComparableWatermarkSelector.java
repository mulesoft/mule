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
     * Returns an int value according to the {@link Comparable} contract (-1, 0, 1).
     * Then the result of the comparation matches this method's return value, then
     * the selected value is updated
     */
    protected abstract int comparableQualifier();

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
            Comparable<Object> current = (Comparable<Object>) this.value;

            if (current == null
                || normalizeValue(((Comparable<Object>) value).compareTo(current)) == this.comparableQualifier())
            {
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

    /**
     * A method used to normalize the value given back by the compareTo method. The
     * Min and Max selectors expect Java's compareTo method to return -1 or 1
     * respectively. However, this is not always the case, so we use this method
     * change and negative result to -1, and any positive result to 1.
     * 
     * @param value The result of a compareTo method.
     * @return -1 if value is less than 0, 1 if value is larger than 0
     */
    private int normalizeValue(int value)
    {
        if (value > 0)
        {
            return 1;
        }
        if (value < 0)
        {
            return -1;
        }
        return 0;
    }
}
