/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.components.script.refreshable;

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
    private static final Class[] UMOEVENTCONTEXT = new Class[]{MuleEventContext.class};
    
    public GroovyRefreshableBeanBuilder()
    {
        super();
    }

    public Object onCall(MuleEventContext eventContext) throws Exception
    {
        if (refreshableBean instanceof GroovyObject)
        {
            GroovyObject script = (GroovyObject)refreshableBean;
            MetaMethod onCall = script.getMetaClass().pickMethod("onCall", UMOEVENTCONTEXT);

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
                
                return script.invokeMethod(methodName, eventContext.transformMessage());
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


