/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf.config;

import java.util.HashMap;
import java.util.Map;

public class WsConfig
{
    Map<String, Object> configProperties = new HashMap<String, Object> ();

    public WsConfig()
    {

    }

    public WsConfig(Map<String, Object> configProperties)
    {
        this.configProperties = configProperties;
    }

    public void setConfigProperties(Map<String, Object> configProperties)
    {
        this.configProperties = configProperties;
    }

    public Map<String, Object> getConfigProperties()
    {
        return configProperties;
    }

}
