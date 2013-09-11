/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.callback;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.callback.HttpCallback;
import org.mule.api.callback.HttpCallbackFactory;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;
import org.mule.security.oauth.DefaultHttpCallback;
import org.mule.security.oauth.processor.ExtractAuthorizationCodeMessageProcessor;
import org.mule.security.oauth.processor.FetchAccessTokenMessageProcessor;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class DefaultHttpCallbackFactory implements HttpCallbackFactory
{

    @Override
    public HttpCallback createCallback(HttpCallbackAdapter adapter,
                                       String authCodeRegex,
                                       FetchAccessTokenMessageProcessor fetchAccessTokenMessageProcessor,
                                       MessageProcessor listener,
                                       MuleContext muleContext,
                                       FlowConstruct flowConstruct) throws MuleException
    {
        return new DefaultHttpCallback(this.buildCallbackProcessorList(
            new ExtractAuthorizationCodeMessageProcessor(Pattern.compile(authCodeRegex)),
            fetchAccessTokenMessageProcessor, listener), muleContext, adapter.getDomain(),
            adapter.getLocalPort(), adapter.getRemotePort(), adapter.getPath(), adapter.getAsync(),
            flowConstruct.getExceptionListener(), adapter.getConnector());
    }
    
    private List<MessageProcessor> buildCallbackProcessorList(MessageProcessor... processors)
    {
        return Arrays.asList(processors);
    }
    
    

}
