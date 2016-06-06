/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.parsers.beans;

import java.util.HashMap;
import java.util.Map;

public class SimpleCollectionObject
{

    private Map<Object, Object> simpleParameters = new HashMap<>();

    public void setSimpleParameters(Map<Object, Object> simpleParameters)
    {
        this.simpleParameters = simpleParameters;
    }

    public Map<Object, Object> getSimpleParameters()
    {
        return simpleParameters;
    }
}
