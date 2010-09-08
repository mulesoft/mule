/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.construct.builder;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.routing.filter.Filter;
import org.mule.construct.Validator;

public class ValidatorBuilder extends
    AbstractFlowConstructWithSingleOutboundEndpointBuilder<ValidatorBuilder, Validator>
{
    protected Filter validationFilter;
    protected String ackExpression;
    protected String nackExpression;

    public ValidatorBuilder validationFilter(Filter validationFilter)
    {
        this.validationFilter = validationFilter;
        return this;
    }

    public ValidatorBuilder ackExpression(String ackExpression)
    {
        this.ackExpression = ackExpression;
        return this;
    }

    public ValidatorBuilder nackExpression(String nackExpression)
    {
        this.nackExpression = nackExpression;
        return this;
    }

    @Override
    protected MessageExchangePattern getInboundMessageExchangePattern()
    {
        return MessageExchangePattern.REQUEST_RESPONSE;
    }

    @Override
    protected MessageExchangePattern getOutboundMessageExchangePattern()
    {
        return MessageExchangePattern.ONE_WAY;
    }

    @Override
    protected Validator buildFlowConstruct(MuleContext muleContext) throws MuleException
    {
        return new Validator(name, muleContext, getOrBuildInboundEndpoint(muleContext),
            getOrBuildOutboundEndpoint(muleContext), validationFilter, ackExpression, nackExpression);
    }
}
