/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.factories;

import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.construct.Validator;
import org.mule.construct.builder.AbstractFlowConstructBuilder;
import org.mule.construct.builder.ValidatorBuilder;
import org.mule.routing.MessageFilter;

public class ValidatorFactoryBean extends AbstractFlowConstructFactoryBean
{
    final ValidatorBuilder validatorBuilder = new ValidatorBuilder();

    @Override
    protected AbstractFlowConstructBuilder<ValidatorBuilder, Validator> getFlowConstructBuilder()
    {
        return validatorBuilder;
    }

    public Class<?> getObjectType()
    {
        return Validator.class;
    }

    public void setEndpoint(OutboundEndpoint endpoint)
    {
        validatorBuilder.outboundEndpoint(endpoint);
    }

    public void setMessageProcessor(MessageProcessor processor)
    {
        if (processor instanceof MessageFilter)
        {
            validatorBuilder.validationFilter(((MessageFilter) processor).getFilter());
        }
        else if (processor instanceof OutboundEndpoint)
        {
            validatorBuilder.outboundEndpoint((OutboundEndpoint) processor);
        }
        else
        {
            throw new IllegalArgumentException("Unsupported message processor: " + processor);
        }
    }

    public void setInboundAddress(String inboundAddress)
    {
        validatorBuilder.inboundAddress(inboundAddress);
    }

    public void setInboundEndpoint(EndpointBuilder inboundEndpointBuilder)
    {
        validatorBuilder.inboundEndpoint(inboundEndpointBuilder);
    }

    public void setOutboundAddress(String outboundAddress)
    {
        validatorBuilder.outboundAddress(outboundAddress);
    }

    public void setOutboundEndpoint(EndpointBuilder outboundEndpointBuilder)
    {
        validatorBuilder.outboundEndpoint(outboundEndpointBuilder);
    }

    public void setAckExpression(String ackExpression)
    {
        validatorBuilder.ackExpression(ackExpression);
    }

    public void setNackExpression(String nackExpression)
    {
        validatorBuilder.nackExpression(nackExpression);
    }

    public void setErrorExpression(String errorExpression)
    {
        validatorBuilder.errorExpression(errorExpression);
    }

    public void setValidationFilter(Filter validationFilter)
    {
        validatorBuilder.validationFilter(validationFilter);
    }
}
