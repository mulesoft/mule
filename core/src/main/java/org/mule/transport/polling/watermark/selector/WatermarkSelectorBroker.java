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
        public WatermarkSelector newSelector(String selectorExpression)
        {
            return new MinValueWatermarkSelector(selectorExpression);
        }
    },
    MAX
    {

        @Override
        public WatermarkSelector newSelector(String selectorExpression)
        {
            return new MaxValueWatermarkSelector(selectorExpression);
        }
    },
    FIRST
    {

        @Override
        public WatermarkSelector newSelector(String selectorExpression)
        {
            return new FirstItemWatermarkSelector(selectorExpression);
        }
    },
    LAST
    {

        @Override
        public WatermarkSelector newSelector(String selectorExpression)
        {
            return new LastItemWatermarkSelector(selectorExpression);
        }
    };

    /**
     * Returns a new instance of {@link WatermarkSelector}. Each invokation returns a
     * different instance
     * 
     * @param selectorExpression the expression the selector is going to use to
     *            evaluate
     * @return a {@link WatermarkSelector}
     */
    public abstract WatermarkSelector newSelector(String selectorExpression);
}
