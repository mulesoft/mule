/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.security;


import static org.apache.ws.security.handler.WSHandlerConstants.PW_CALLBACK_REF;

import java.util.Map;

import org.apache.ws.security.handler.WSHandlerConstants;

public abstract class AbstractSecurityStrategy implements SecurityStrategy
{

    /**
     * Adds an action to a CXF config map.
     */
    protected void appendAction(Map<String, Object> configProperties, String action)
    {
        String previousAction = "";

        if (configProperties.containsKey(WSHandlerConstants.ACTION))
        {
            previousAction = configProperties.get(WSHandlerConstants.ACTION) + " ";
        }
        configProperties.put(WSHandlerConstants.ACTION, previousAction + action);
    }

    /**
     * Adds a password callback to a config properties map, allowing to compose many handlers from different security strategies.
     */
    protected void addPasswordCallbackHandler(Map<String, Object> configProperties, WSPasswordCallbackHandler handler)
    {
        CompositeCallbackHandler compositeCallbackHandler = (CompositeCallbackHandler) configProperties.get(PW_CALLBACK_REF);

        if (compositeCallbackHandler == null)
        {
            compositeCallbackHandler = new CompositeCallbackHandler();
            configProperties.put(PW_CALLBACK_REF, compositeCallbackHandler);
        }

        compositeCallbackHandler.addCallbackHandler(handler);
    }
}
