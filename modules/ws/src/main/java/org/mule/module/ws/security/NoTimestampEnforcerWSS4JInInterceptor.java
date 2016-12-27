/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;

/**
 * This class was included so that when no timestamp is received in response to a request that was sent with a
 * timestamp, no exception is raised due to no matching between actions performed in the request and actions requested
 * in the response for wss. 
 */
public class NoTimestampEnforcerWSS4JInInterceptor extends WSS4JInInterceptor
{

    public NoTimestampEnforcerWSS4JInInterceptor(Map<String, Object> inConfigProperties)
    {
        super(inConfigProperties);
    }

    protected boolean checkReceiverResultsAnyOrder(List<WSSecurityEngineResult> wsResult, List<Integer> actions)
    {
        List<Integer> recordedActions = new ArrayList<Integer>(actions.size());
        for (Integer action : actions)
        {
            recordedActions.add(action);
        }

        for (WSSecurityEngineResult result : wsResult)
        {
            final Integer actInt = (Integer) result.get(WSSecurityEngineResult.TAG_ACTION);
            int act = actInt.intValue();
            if (act == WSConstants.SC || act == WSConstants.BST || act == WSConstants.TS)
            {
                continue;
            }

            if (!recordedActions.remove(actInt))
            {
                return false;
            }
        }

        if (!recordedActions.isEmpty())
        {
            return false;
        }

        return true;
    }
}
