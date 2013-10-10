/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ibean;

import org.mule.api.MuleContext;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.endpoint.ResourceNameEndpointURIBuilder;
import org.mule.module.ibeans.config.IBeanHolder;
import org.mule.module.ibeans.i18n.IBeansMessages;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Properties;

import org.ibeans.annotation.Call;
import org.ibeans.annotation.Template;

/**
 * A Resource name endpoint builder that will check the validity of an iBean endpoint by looking up the
 * iBean and checking the method exists on the iBean and pointing to a valid method (i.e. with a {@link org.ibeans.annotation.Call} or {@link org.ibeans.annotation.Template} annotation)
 */
public class IBeansEndpointURIBuilder extends ResourceNameEndpointURIBuilder
{
    private MuleContext muleContext;

    @Override
    protected void setEndpoint(URI uri, Properties props) throws MalformedEndpointException
    {
        super.setEndpoint(uri, props);
        //Validate the iBean name and method
        int i = address.indexOf(".");
        if(i ==-1)
        {
            throw new MalformedEndpointException(uri.toString());
        }

        String ibean = address.substring(0, i);
        String method = address. substring(i+1);
        IBeanHolder holder = muleContext.getRegistry().lookupObject(ibean);
        if(holder == null)
        {
            throw new MalformedEndpointException(IBeansMessages.ibeanNotRegistered(ibean), uri.toString());
        }
        boolean match = false;
        Method[] methods = holder.getIbeanClass().getMethods();
        for (int j = 0; j < methods.length; j++)
        {
            Method m = methods[j];
            if(m.getName().equals(method))
            {
                if(m.isAnnotationPresent(Call.class) || m.isAnnotationPresent(Template.class))
                {
                    match = true;
                    break;
                }
                else
                {
                    throw new MalformedEndpointException(IBeansMessages.ibeanMethodFoundButNotValid(ibean, method), uri.toString());
                }
            }
        }
        if(!match)
        {
            throw new MalformedEndpointException(IBeansMessages.ibeanMethodNotFound(ibean, method), uri.toString());
        }
    }

    @Override
    public EndpointURI build(URI uri, MuleContext muleContext) throws MalformedEndpointException
    {
        this.muleContext = muleContext;
        return super.build(uri, muleContext);
    }
}
