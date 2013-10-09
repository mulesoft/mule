/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf;

import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.transport.NullPayload;

/**
 * This enum defines the strategies to convert a Payload to an array of arguments
 * that will be used to call the webservice in
 * {@link CxfOutboundMessageProcessor#doSendWithClient(org.mule.api.MuleEvent)} and in
 * {@link CxfOutboundMessageProcessor#doSendWithProxy(org.mule.api.MuleEvent)}.
 */
public enum CxfPayloadToArguments
{
    /**
     * In this strategy, if the payload is of type {@link NullPayload} it will be
     * send as a parameter just like any other object.
     */
    NULL_PAYLOAD_AS_PARAMETER(CxfConstants.PAYLOAD_TO_ARGUMENTS_NULL_PAYLOAD_AS_PARAMETER)
    {

    },
    /**
     * In this strategy, if the payload is of type {@link NullPayload} it will not be
     * send as a parameter. The array of arguments in this case will be empty. For
     * the rest of the objects it behaves just like
     * {@link #NULL_PAYLOAD_AS_PARAMETER} (it will delegate to
     * {@link CxfPayloadToArguments#payloadToArrayOfArguments(Object)}).
     */
    NULL_PAYLOAD_AS_VOID(CxfConstants.PAYLOAD_TO_ARGUMENTS_NULL_PAYLOAD_AS_VOID)
    {
        @Override
        public Object[] payloadToArrayOfArguments(Object payload)
        {
            if (payload instanceof NullPayload)
            {
                return new Object[]{};
            }
            else
            {
                return super.payloadToArrayOfArguments(payload);
            }
        }
    };

    /**
     * This is the value that is needed to be configured in the endpoint under
     * property {@link CxfConstants#PAYLOAD_TO_ARGUMENTS} so this
     * {@link CxfPayloadToArguments} is selected on method
     * {@link #getPayloadToArgumentsForEndpoint(OutboundEndpoint)}.
     */
    private final String payloadToArgumentsParameterValue;

    private CxfPayloadToArguments(String payloadToArgumentsParameterValue)
    {
        this.payloadToArgumentsParameterValue = payloadToArgumentsParameterValue;
    }

    /**
     * This method is the one that converts the payload in an array of arguments. In
     * this default implementation if the payload is already an array of
     * {@link Object objects} that array will be returned. Otherwise, an array with
     * one element, the payload, will be returned.
     * 
     * @param payload the payload to convert to array of arguments.
     * @return the array of arguments
     */
    public Object[] payloadToArrayOfArguments(Object payload)
    {
        Object[] args;
        if (payload instanceof Object[])
        {
            args = (Object[]) payload;
        }
        else
        {
            args = new Object[]{payload};
        }
        return args;
    }

    public String getPayloadToArgumentsParameterValue()
    {
        return payloadToArgumentsParameterValue;
    }
}
