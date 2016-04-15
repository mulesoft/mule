/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.xml.ws.handler.MessageContext.Scope;

import org.apache.cxf.jaxws.JAXWSMethodInvoker;
import org.apache.cxf.jaxws.context.WebServiceContextImpl;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.invoker.Invoker;

public class MuleJAXWSInvoker extends JAXWSMethodInvoker
{
    private Invoker muleInvoker;

    public MuleJAXWSInvoker(Invoker muleInvoker)
    {
        super(new Object());
        this.muleInvoker = muleInvoker;
    }

    @Override
    protected Object invoke(Exchange exchange, final Object serviceObject, Method m, List<Object> params)
    {
        // set up the webservice request context
        WrappedMessageContext ctx = new WrappedMessageContext(exchange.getInMessage(), Scope.APPLICATION);

        Map<String, Object> handlerScopedStuff = removeHandlerProperties(ctx);

        WebServiceContextImpl.setMessageContext(ctx);
        Object res = null;
        try
        {
            res = muleInvoker.invoke(exchange, serviceObject);
            addHandlerProperties(ctx, handlerScopedStuff);
            // update the webservice response context
            updateWebServiceContext(exchange, ctx);
        }
        finally
        {
            // clear the WebServiceContextImpl's ThreadLocal variable
            WebServiceContextImpl.clear();
        }
        return res;
    }
}
