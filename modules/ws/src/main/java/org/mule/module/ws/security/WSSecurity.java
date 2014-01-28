/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.security;

import org.mule.util.Preconditions;

import java.util.ArrayList;
import java.util.List;

public class WSSecurity
{

    private List<SecurityStrategy> strategies = new ArrayList<SecurityStrategy>();

    public List<SecurityStrategy> getStrategies()
    {
        return strategies;
    }

    public void setStrategies(List<SecurityStrategy> strategies)
    {
        Preconditions.checkArgument(strategies != null, "Strategy list cannot be null");
        this.strategies = strategies;
    }

    public boolean hasStrategies()
    {
        return !strategies.isEmpty();
    }

}
