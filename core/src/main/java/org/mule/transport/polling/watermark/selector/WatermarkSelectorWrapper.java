/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.polling.watermark.selector;

import org.mule.DefaultMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.transport.polling.watermark.WatermarkUtils;

import java.io.NotSerializableException;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WatermarkSelectorWrapper extends WatermarkSelector
{

    private static final Logger logger = LoggerFactory.getLogger(WatermarkSelectorWrapper.class);

    private final String selectorExpression;
    private final WatermarkSelector wrapped;
    private final MuleEvent muleEvent;

    protected WatermarkSelectorWrapper(WatermarkSelector wrapped, String selectorExpression, MuleEvent muleEvent)
    {
        this.selectorExpression = selectorExpression;
        this.wrapped = wrapped;
        this.muleEvent = DefaultMuleEvent.copy(muleEvent);
    }

    @Override
    public void acceptValue(Object value)
    {
        this.muleEvent.getMessage().setPayload(value);
        try
        {
            Serializable evaluated = WatermarkUtils.evaluate(this.selectorExpression, muleEvent);
            this.wrapped.acceptValue(evaluated);
        }
        catch (NotSerializableException e)
        {
            logger.warn(
                String.format(
                    "Watermark selector expression '%s' did not resolved to a Serializable value. Value will be ignored",
                    this.selectorExpression), e);
        }
    }

    @Override
    public Object getSelectedValue()
    {
        return this.wrapped.getSelectedValue();
    }

    @Override
    public void reset()
    {
        this.wrapped.reset();
    }

}
