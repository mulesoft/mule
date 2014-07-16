/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.config;

import org.mule.module.cxf.support.MuleSecurityManagerValidator;

import java.util.HashMap;
import java.util.Map;

public class WsSecurity
{
    String name;
    WsSecurity ref;
    WsConfig wsConfig;
    MuleSecurityManagerValidator securityManager;
    Map<String, Object> customValidator;

    public void setRef(WsSecurity ref)
    {
        this.ref = ref;
    }
    
    public WsSecurity getRef()
    {
        return ref;
    }

    public void setWsConfig(WsConfig wsConfig)
    {
        this.wsConfig = wsConfig;
    }

    public Map<String, Object> getConfigProperties()
    {
        if(ref != null)
        {
            return ref.getConfigProperties();
        }
        if(wsConfig != null)
        {
            return wsConfig.getConfigProperties();
        }
        return new HashMap<String, Object>();
    }
    
    public MuleSecurityManagerValidator getSecurityManager()
    {
        if(ref != null)
        {
            return ref.getSecurityManager();
        }
        return securityManager;
    }
    
    public void setName(String name)
    {
        this.name = name;
    }
    
    public String getName()
    {
        if(ref != null)
        {
            return ref.getName();
        }
        return name;
    }

    public void setSecurityManager(MuleSecurityManagerValidator securityManager)
    {
        this.securityManager = securityManager;
    }
    
    public void setCustomValidator(Map<String, Object> customValidator)
    {
        this.customValidator = customValidator;
    }
    
    public Map<String, Object> getCustomValidator()
    {
        if(ref != null)
        {
            return ref.getCustomValidator();
        }
        return customValidator;
    }
    

}
