/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.security;

import static com.google.common.collect.Lists.newLinkedList;

import java.io.IOException;
import java.util.List;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * Callback handler implementation that delegates the handle operation to a list of callback handlers.
 * This allows to compose multiple callback handler implementations to handle different types of callbacks.
 */
public class CompositeCallbackHandler implements CallbackHandler
{
    private List<CallbackHandler> callbackHandlers = newLinkedList();

    public void addCallbackHandler(CallbackHandler callbackHandler)
    {
        callbackHandlers.add(callbackHandler);
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException
    {
        for (CallbackHandler callbackHandler : callbackHandlers)
        {
            callbackHandler.handle(callbacks);
        }
    }

}
