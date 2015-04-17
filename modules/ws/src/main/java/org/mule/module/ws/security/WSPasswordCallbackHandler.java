/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.security;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.ws.security.WSPasswordCallback;

/**
 * Abstract implementation of {@link javax.security.auth.callback.CallbackHandler} that only handles instances
 * of {@link org.apache.ws.security.WSPasswordCallback} with a specific usage.
 */
public abstract class WSPasswordCallbackHandler implements CallbackHandler
{
    private final int usage;

    /**
     * @param usage A constant from {@link org.apache.ws.security.WSPasswordCallback} indicating the usage of this callback.
     */
    public WSPasswordCallbackHandler(int usage)
    {
        this.usage = usage;
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException
    {
        for (Callback callback : callbacks)
        {
            if (callback instanceof WSPasswordCallback)
            {
                WSPasswordCallback passwordCallback = (WSPasswordCallback) callback;
                if (passwordCallback.getUsage() == usage)
                {
                    handle(passwordCallback);
                }
            }
        }
    }

    /**
     * Handles a password callback. This method will be called with the password callback that matches
     * the usage provided in the constructor of this class.
     */
    public abstract void handle(WSPasswordCallback passwordCallback);

}
