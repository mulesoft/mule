/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.callback;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;
import org.mule.security.oauth.callback.HttpCallbackAdapter;
import org.mule.security.oauth.processor.FetchAccessTokenMessageProcessor;

/**
 * Factory class for instances of {@link org.mule.api.callback.HttpCallback} that are
 * used on the OAuth dance
 */
public interface HttpCallbackFactory
{

    /**
     * returns a new callback that has not been started
     * 
     * @param adapter adapter holding the callback configuration
     * @param authCodeRegex uncompiled regular expression to extract the
     *            authorization code
     * @param fetchAccessTokenMessageProcessor an instance of
     *            {@link org.mule.security.oauth.processor.FetchAccessTokenMessageProcessor}
     * @param listener a message processor to invoke when the callback has been
     *            received
     * @param muleContext a {@link MuleContext}
     * @param flowConstruct the construct of the flow where this callback lives
     * @return
     * @throws MuleException
     */
    public HttpCallback createCallback(HttpCallbackAdapter adapter,
                                       String authCodeRegex,
                                       FetchAccessTokenMessageProcessor fetchAccessTokenMessageProcessor,
                                       MessageProcessor listener,
                                       MuleContext muleContext,
                                       FlowConstruct flowConstruct) throws MuleException;

    public void forceOldHttpTransport(boolean forceOld);

}
