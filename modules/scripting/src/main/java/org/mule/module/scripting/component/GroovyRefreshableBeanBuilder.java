/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.scripting.component;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.StringUtils;

import groovy.lang.GroovyObject;
import groovy.lang.MetaMethod;

public class GroovyRefreshableBeanBuilder implements Callable
{
    private volatile Object refreshableBean;
    private String methodName;
    private static final String ON_CALL = "onCall";
    private static final Class[] MULE_EVENT_CONTEXT = new Class[]{MuleEventContext.class};

    public GroovyRefreshableBeanBuilder()
    {
        super();
    }

    public Object onCall(MuleEventContext eventContext) throws Exception
    {
        if (refreshableBean instanceof GroovyObject)
        {
            GroovyObject script = (GroovyObject)refreshableBean;
            MetaMethod onCall = script.getMetaClass().pickMethod("onCall", MULE_EVENT_CONTEXT);

            if (onCall != null)
            {
                return script.invokeMethod(ON_CALL, eventContext);
            }
            else
            {
                if (StringUtils.isEmpty(methodName))
                {
                    throw new DefaultMuleException(CoreMessages.propertiesNotSet("methodName"));
                }
                
                return script.invokeMethod(methodName, eventContext.getMessage().getPayload());
            }
            
        }
        
        throw new Exception(new DefaultMuleException("script engine not supported"));
    }

    public Object getRefreshableBean()
    {
        return refreshableBean;
    }

    public void setRefreshableBean(Object refreshableBean)
    {
        this.refreshableBean = refreshableBean;
    }
    
    public String getMethodName()
    {
        return methodName;
    }

    public void setMethodName(String methodName)
    {
        this.methodName = methodName;
    }
}


