/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


