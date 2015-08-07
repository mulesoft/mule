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
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.security.oauth.DefaultHttpCallback;
import org.mule.security.oauth.processor.ExtractAuthorizationCodeMessageProcessor;
import org.mule.security.oauth.processor.FetchAccessTokenMessageProcessor;

import java.util.regex.Pattern;

public class DefaultHttpCallbackFactory implements HttpCallbackFactory
{
    private boolean forceOldHttpTransport = false;

    @Override
    public HttpCallback createCallback(HttpCallbackAdapter adapter,
                                       String authCodeRegex,
                                       FetchAccessTokenMessageProcessor fetchAccessTokenMessageProcessor,
                                       MessageProcessor listener,
                                       MuleContext muleContext,
                                       FlowConstruct flowConstruct) throws MuleException
    {
        DefaultHttpCallback callback = new DefaultHttpCallback(this.buildCallbackProcessor(authCodeRegex,
            fetchAccessTokenMessageProcessor, listener), muleContext, adapter.getDomain(),
            adapter.getLocalPort(), adapter.getRemotePort(), adapter.getPath(), adapter.getAsync(),
            flowConstruct.getExceptionListener(), adapter.getConnector());

        callback.setForceOldHttpTransport(forceOldHttpTransport);
        return callback;
    }

    private MessageProcessor buildCallbackProcessor(String authCodeRegex,
                                                    FetchAccessTokenMessageProcessor fetchAccessTokenMessageProcessor,
                                                    MessageProcessor listener) throws MuleException
    {
        return new DefaultMessageProcessorChainBuilder().chain(
            new  ExtractAuthorizationCodeMessageProcessor(Pattern.compile(authCodeRegex)),
            fetchAccessTokenMessageProcessor, new CallbackContinuationMessageProcessor(listener)).build();
    }

    @Override
    public void forceOldHttpTransport(boolean forceOld)
    {
        forceOldHttpTransport = forceOld;
    }
}
