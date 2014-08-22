/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey.contextresolver;

import org.mule.module.jersey.HelloBean;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.glassfish.jersey.jettison.JettisonConfig;
import org.glassfish.jersey.jettison.JettisonJaxbContext;

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
                    context = new JettisonJaxbContext(JettisonConfig.DEFAULT, type);
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
