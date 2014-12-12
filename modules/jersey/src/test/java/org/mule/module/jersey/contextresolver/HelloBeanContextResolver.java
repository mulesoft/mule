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

@Provider
public class HelloBeanContextResolver implements ContextResolver<HelloBean>
{

    private int index = 0;

    @Override
    public HelloBean getContext(Class<?> type)
    {
        if (type == HelloBean.class)
        {
            HelloBean helloBean = new HelloBean();
            helloBean.setMessage("from contextResolver");
            helloBean.setNumber(index++);

            return helloBean;
        }

        return null;
    }
}
