/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.config;

import org.mule.api.routing.filter.Filter;

import org.springframework.beans.factory.FactoryBean;

/**
 * TODO
 */
public class ErrorFilterFactoryBean implements FactoryBean
{
    private Filter filter;

    public Object getObject() throws Exception
    {
        return filter;
    }


    public Class getObjectType()
    {
        return Filter.class;
    }

    public boolean isSingleton()
    {
        return true;
    }

    public Filter getFilter()
    {
        return filter;
    }

    public void setFilter(Filter filter)
    {
        this.filter = filter;
    }
}
