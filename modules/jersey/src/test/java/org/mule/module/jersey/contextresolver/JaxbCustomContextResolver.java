/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jersey.contextresolver;

import org.mule.module.jersey.HelloBean;

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

@Provider
public class JaxbCustomContextResolver implements ContextResolver<JAXBContext>
{
    protected JAXBContext context;

    @Override
    public JAXBContext getContext(Class<?> type)
    {
        if (type.equals(HelloBean.class))
        {
            if (context == null)
            {
                try
                {
                    context = new JSONJAXBContext(JSONConfiguration.natural().build(), type);
                }
                catch (JAXBException e)
                {
                    // do nothing
                }
            }
            return context;
        }

        return null;

    }
}
