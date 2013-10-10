/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
