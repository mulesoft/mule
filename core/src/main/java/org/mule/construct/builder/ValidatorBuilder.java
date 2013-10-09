/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.construct.builder;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.routing.filter.Filter;
import org.mule.construct.Validator;
import org.mule.util.StringUtils;

public class ValidatorBuilder extends
    AbstractFlowConstructWithSingleInboundAndOutboundEndpointBuilder<ValidatorBuilder, Validator>
{
    protected Filter validationFilter;
    protected String ackExpression;
    protected String nackExpression;
    protected String errorExpression;

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

    public ValidatorBuilder errorExpression(String errorExpression)
    {
        this.errorExpression = errorExpression;
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
        return hasErrorExpression()
                                   ? MessageExchangePattern.REQUEST_RESPONSE
                                   : MessageExchangePattern.ONE_WAY;
    }

    @Override
    protected Validator buildFlowConstruct(MuleContext muleContext) throws MuleException
    {
        return new Validator(name, muleContext, getOrBuildInboundEndpoint(muleContext),
            getOrBuildOutboundEndpoint(muleContext), validationFilter, ackExpression, nackExpression,
            errorExpression);
    }

    protected boolean hasErrorExpression()
    {
        return StringUtils.isNotBlank(errorExpression);
    }
}
