/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.polling.watermark.selector;

/**
 * Broker for the default implementations of {@link WatermarkSelector}
 * 
 * @since 3.5.0
 */
public enum WatermarkSelectorBroker
{

    MIN
    {

        @Override
        public WatermarkSelector newSelector()
        {
            return new MinValueWatermarkSelector();
        }
    },
    MAX
    {

        @Override
        public WatermarkSelector newSelector()
        {
            return new MaxValueWatermarkSelector();
        }
    },
    FIRST
    {

        @Override
        public WatermarkSelector newSelector()
        {
            return new FirstItemWatermarkSelector();
        }
    },
    LAST
    {

        @Override
        public WatermarkSelector newSelector()
        {
            return new LastItemWatermarkSelector();
        }
    };

    /**
     * Returns a new instance of {@link WatermarkSelector}. Each invocation returns a
     * different instance
     * 
     * @return a {@link WatermarkSelector}
     */
    public abstract WatermarkSelector newSelector();
}
