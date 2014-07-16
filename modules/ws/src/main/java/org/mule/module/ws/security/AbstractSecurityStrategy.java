/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.security;


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

}
