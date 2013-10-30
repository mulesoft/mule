/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.jersey.contextresolver;

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import org.mule.module.jersey.HelloBean;
import org.mule.module.jersey.HelloWorld;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

@Provider
public class HelloWorldContextResolver implements ContextResolver<JAXBContext>
{
    protected JAXBContext context;

    @Override
    public JAXBContext getContext(Class<?> type)
    {
        if (type.equals(HelloWorld.class))
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
