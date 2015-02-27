/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal;

import org.mule.api.MuleEvent;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import java.util.List;

public class HttpMessageBuilder implements Initialisable
{

    private String name;
    protected Multimap<HttpParamType, HttpParam> params = ArrayListMultimap.create();

    public void setParams(List<HttpParam> httpParams)
    {
        for (HttpParam httpParam : httpParams)
        {
            params.put(httpParam.getType(), httpParam);
        }
    }


    @Override
    public void initialise() throws InitialisationException
    {
        LifecycleUtils.initialiseIfNeeded(params.values());
    }

    public ParameterMap resolveParams(MuleEvent muleEvent, HttpParamType httpParamType)
    {
        Iterable<HttpParam> paramList = params.get(httpParamType);
        ParameterMap httpParams = new ParameterMap();

        for (HttpParam httpParam : paramList)
        {
            httpParam.resolve(httpParams, muleEvent);
        }

        return httpParams;
    }

    public void setBuilders(List<HttpMessageBuilderRef> httpBuilderRefs)
    {
        for (HttpMessageBuilderRef httpBuilderRef : httpBuilderRefs)
        {
            setParams(Lists.newArrayList(httpBuilderRef.getRef().params.values()));
        }
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void addHeader(String headerName, String headerValue)
    {
        final HttpSingleParam httpSingleParam = new HttpSingleParam(HttpParamType.HEADER);
        httpSingleParam.setName(headerName);
        httpSingleParam.setValue(headerValue);
        this.params.put(HttpParamType.HEADER, httpSingleParam);
    }

}
