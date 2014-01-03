/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.security;

import java.util.List;

public class WSSecurity
{

    protected List<SecurityStrategy> strategies;

    public List<SecurityStrategy> getStrategies()
    {
        return strategies;
    }

    public void setStrategies(List<SecurityStrategy> strategies)
    {
        this.strategies = strategies;
    }

}
