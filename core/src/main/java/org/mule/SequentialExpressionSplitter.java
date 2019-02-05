package org.mule;
/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
import org.mule.api.MuleMessage;
import org.mule.expression.ExpressionConfig;
import org.mule.routing.CorrelationMode;
import org.mule.routing.ExpressionSplitter;

public class SequentialExpressionSplitter extends ExpressionSplitter
{
    public SequentialExpressionSplitter(ExpressionConfig config)
    {
        super(config);
    }

    @Override
    protected void setCorrelationParameters(MuleMessage message, String correlationId, int count, int correlationSequence)
    {
        if (enableCorrelation != CorrelationMode.NEVER)
        {
            boolean correlationSet = message.getCorrelationId() != null;
            if ((!correlationSet && (enableCorrelation == CorrelationMode.IF_NOT_SET))
                    || (enableCorrelation == CorrelationMode.ALWAYS))
            {
                message.setCorrelationId(correlationId + "-" + correlationSequence);
            }
            // take correlation group size from the message properties, set by
            // concrete
            // message splitter implementations
            message.setCorrelationGroupSize(count);
            message.setCorrelationSequence(correlationSequence);
        }
    }
}
