/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener.matcher;

import org.mule.module.http.internal.domain.request.HttpRequest;
import org.mule.util.Preconditions;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;

/**
 * Request matcher for http methods.
 */
public class MethodRequestMatcher implements RequestMatcher
{

    private final List<String> methods;

    /**
     * The list of methods accepted by this matcher
     *
     * @param methods http request method allowed.
     */
    public MethodRequestMatcher(final String... methods)
    {
        Preconditions.checkArgument(methods != null, "methods attribute should not be null");
        this.methods = (List<String>) CollectionUtils.collect(Arrays.asList(methods), new Transformer()
        {
            @Override
            public Object transform(Object input)
            {
                return ((String) input).toLowerCase();
            }
        });
    }

    /**
     * Determines if there's an intersection between the allowed methods by two
     * request matcher
     *
     * @param methodRequestMatcher request matcher to test against
     * @return true at least there's one http method that both request matcher accepts, false otherwise.
     */
    public boolean intersectsWith(final MethodRequestMatcher methodRequestMatcher)
    {
        Preconditions.checkArgument(methodRequestMatcher != null, "methodRequestMatcher cannot be null");
        for (String method : methods)
        {
            if (methodRequestMatcher.methods.contains(method))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean matches(final HttpRequest httpRequest)
    {
        return this.methods.contains(httpRequest.getMethod().toLowerCase());
    }

    @Override
    public String toString()
    {
        return "MethodRequestMatcher{" +
               "methods=" + getMethodsList() +
               '}';
    }

    public String getMethodsList()
    {
        return methods.isEmpty() ? "*" : Arrays.toString(methods.toArray());
    }
}