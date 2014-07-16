/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.param;

import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Fruit;

import java.util.List;
import java.util.Map;

/**
 * Tests various cases for how headers can be injected into a component invocation
 */
public class InboundHeadersAnnotationComponent
{
    public String processHeader(@InboundHeaders("foo") String foo)
    {
        return foo;
    }

    public String processHeaderOptional(@InboundHeaders("faz?") String faz)
    {
        if(faz==null)
        {
            return "faz not set";
        }
        return faz;
    }

    public Apple processHeaderWithType(@InboundHeaders("apple") Apple apple)
    {
        return apple;
    }

    public Fruit processHeaderWithBaseType(@InboundHeaders("apple") Fruit apple)
    {
        return apple;
    }

    public Map<?, ?> processHeaders(@InboundHeaders("foo, bar") Map<?, ?> headers)
    {
        return headers;
    }

    public Map<?, ?> processHeadersAll(@InboundHeaders("*") Map<?, ?> headers)
    {
        return headers;
    }

    public Map<?, ?> processHeadersWildcard(@InboundHeaders("MULE_*") Map<?, ?> headers)
    {
        return headers;
    }

    public Map<?, ?> processHeadersMultiWildcard(@InboundHeaders("MULE_*, ba*") Map<?, ?> headers)
    {
        return headers;
    }

    public Map<?, ?> processSingleMapHeader(@InboundHeaders("foo") Map<?, ?> headers)
    {
        return headers;
    }

    public Map<?, ?> processHeadersOptional(@InboundHeaders("foo, bar, baz?") Map<?, ?> headers)
    {
        return headers;
    }

    public Map<?, ?> processHeadersAllOptional(@InboundHeaders("foo?, bar?") Map<?, ?> headers)
    {
        return headers;
    }


    public Map<?, ?> processUnmodifiableHeaders(@InboundHeaders("foo, bar") Map<String, Object> headers)
    {
        //Should throw UnsupportedOperationException
        headers.put("car", "carValue");
        return headers;
    }

    public Map<String, Fruit> processHeadersWithGenerics(@InboundHeaders("apple, orange") Map<String, Fruit> headers)
    {
        return headers;
    }

    public List<?> processHeadersList(@InboundHeaders("foo, bar, baz") List<?> headers)
    {
        return headers;
    }

    public List<?> processHeadersListAll(@InboundHeaders("*") List<?> headers)
    {
        return headers;
    }

    public List<?> processSingleHeaderList(@InboundHeaders("foo") List<?> headers)
    {
        return headers;
    }

    public List<?> processHeadersListOptional(@InboundHeaders("foo, bar, baz?") List<?> headers)
    {
        return headers;
    }

    public List<?> processHeadersListAllOptional(@InboundHeaders("foo?, bar?") List<?> headers)
    {
        return headers;
    }

    public List<?> processUnmodifiableHeadersList(@InboundHeaders("foo, bar") List<Object> headers)
    {
        //Should throw UnsupportedOperationException
        headers.add("carValue");
        return headers;
    }

    public List<?> processHeadersListWildcard(@InboundHeaders("MULE_*") List<?> headers)
    {
        return headers;
    }

    public List<?> processHeadersListMultiWildcard(@InboundHeaders("MULE_*, ba*") List<?> headers)
    {
        return headers;
    }

    public List<Fruit> processHeadersListWithGenerics(@InboundHeaders("apple, orange") List<Fruit> headers)
    {
        return headers;
    }
}
