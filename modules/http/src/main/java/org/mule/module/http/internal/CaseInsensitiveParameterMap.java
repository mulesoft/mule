/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal;

import org.mule.util.CaseInsensitiveMapWrapper;

import java.util.LinkedHashMap;
import java.util.LinkedList;

public class CaseInsensitiveParameterMap extends ParameterMap
{
    public CaseInsensitiveParameterMap(ParameterMap paramsMap)
    {
        this.paramsMap = new CaseInsensitiveMapWrapper<>(LinkedHashMap.class);
        for (String key : paramsMap.keySet())
        {
            this.paramsMap.put(key, new LinkedList<>(paramsMap.getAll(key)));
        }
    }
}
