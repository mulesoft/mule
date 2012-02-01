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

import org.mule.module.cxf.support.MuleSecurityManagerValidator;

import java.util.Map;

public class WsSecurity
{
    String name;
    WsSecurity ref;
    Map<String, Object> configProperties;
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
    
    public Map<String, Object> getConfigProperties()
    {
        if(ref != null)
        {
            return ref.getConfigProperties();
        }
        return configProperties;
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
    
    public void setConfigProperties(Map<String, Object> configProperties)
    {
        this.configProperties = configProperties;
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
