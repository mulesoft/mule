/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.spi;

import org.mule.api.MuleContext;
import org.mule.transport.http.HttpConstants;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.DataSource;
import javax.activation.MimeTypeParseException;

import org.ibeans.api.DataType;
import org.ibeans.api.InvocationContext;
import org.ibeans.api.Response;
import org.ibeans.api.channel.MimeType;
import org.ibeans.impl.support.datatype.DataTypeFactory;
import org.ibeans.impl.test.MockIBean;
import org.ibeans.impl.test.MockMessageCallback;
import org.ibeans.spi.IBeansPlugin;

/**
 * A handler for supporting the {@link org.ibeans.annotation.MockIntegrationBean} annotation. This implementation uses
 * Mockito to mock out the call itself. Mockito can be used in tests to supply response data.
 */
public class MuleMockCallAnnotationHandler extends MuleCallAnnotationHandler implements MockIBean
{
    private Object mock;
    private InvocationContext ctx;
    private MockMessageCallback callback;
    private IBeansPlugin plugin;

    public MuleMockCallAnnotationHandler(MuleContext muleContext, Object mock, IBeansPlugin plugin)
    {
        super(muleContext);
        this.mock = mock;
        this.plugin = plugin;
    }

    @Override
    public Response invoke(InvocationContext ctx) throws Exception
    {
        this.ctx = ctx;
        Method method = ctx.getMethod();
        //Special handling of methods with an ibean prefix, these are called by the the IBeansTestSupport
        //To pass in additional information from the testcase
        Object result;
        if (method.getName().startsWith("ibean"))
        {
            result = method.invoke(this, ctx.getArgs());
        }
        else
        {
            result = ctx.getMethod().invoke(mock, ctx.getArgs());
        }

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(HttpConstants.HEADER_CONTENT_TYPE, (ctx.getInvocationReturnType()==null ?
                    ctx.getReturnType().getMimeType() : ctx.getInvocationReturnType().getMimeType()));
        Response response = (result instanceof Response ? (Response)result : plugin.createResponse(result, props, null));

        //Now handled in the test case
        if (callback != null)
        {
            try
            {
                callback.onMessage(response);
            }
            finally
            {
                //Only run it once
                callback = null;
                this.ctx = null;
            }

        }
        return response;
    }

    public String getScheme(Method method)
    {
        //Default to http, should not make a difference for a Mock iBean
        return "http";
    }

    public void ibeanSetMimeType(MimeType mime) throws MimeTypeParseException
    {
        if (ctx.getIBeanConfig().getReturnType() != null)
        {
            ctx.setInvocationReturnType(DataTypeFactory.create(ctx.getIBeanConfig().getReturnType().getType(), mime));
        }
    }

    public DataType ibeanReturnType()
    {
//        if (invocationContext == null || invocationContext.getReturnType().getType().getName().equals("void"))
//        {
//            return helper.getReturnType();
//        }
//        else
        {
            DataType type = ctx.getIBeanConfig().getReturnType();
            if(type==null)
            {
                type = ctx.getIBeanDefaultConfig().getReturnType();
            }
            return type;
        }
    }

    public Object ibeanUriParam(String name)
    {
//        if (invocationContext == null)
//        {
//            return helper.getDefaultUriParams().get(name);
//        }
//        else
        {
            return ctx.getIBeanConfig().getUriParams().get(name);
        }
    }

    public Object ibeanHeaderParam(String name)
    {
//        if (invocationContext == null)
//        {
//            return helper.getDefaultHeaderParams().get(name);
//        }
//        else
        {
            return ctx.getIBeanConfig().getHeaderParams().get(name);
        }
    }

    public Object ibeanPropertyParam(String name)
    {
//        if (invocationContext == null)
//        {
//            return helper.getDefaultPropertyParams().get(name);
//        }
//        else
        {
            return ctx.getIBeanConfig().getPropertyParams().get(name);
        }
    }

    public Object ibeanPayloadParam(String name)
    {
//        if (invocationContext == null)
//        {
//            return helper.getDefaultPayloadParams().get(name);
//        }
//        else
        {
            return ctx.getIBeanConfig().getPayloadParams().get(name);
        }
    }

    public List<Object> ibeanPayloads()
    {
//        if (invocationContext == null)
//        {
//            return null;
//        }
//        else
        {
            return ctx.getIBeanConfig().getPayloads();
        }
    }

    public Set<DataSource> ibeanAttachments()
    {
//        if (invocationContext == null)
//        {
//            return null;
//        }
//        else
        {
            return ctx.getIBeanConfig().getAttachments();
        }
    }


    public void ibeanSetMessageCallback(MockMessageCallback callback)
    {
        this.callback = callback;
    }

}
