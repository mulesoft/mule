/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener.matcher;

import org.mule.module.http.internal.domain.request.HttpRequest;

/**
 * Request matcher for an http listener that accepts request based
 * on a path and a group of allowed methods.
 */
public class ListenerRequestMatcher implements RequestMatcher
{

    private String path;
    private final MethodRequestMatcher methodRequestMatcher;

    public ListenerRequestMatcher(final MethodRequestMatcher methodRequestMatcher, final String path)
    {
        this.methodRequestMatcher = methodRequestMatcher;
        this.path = path;
        if (doesNotEndWithWildcardPath())
        {
            this.path = this.path + "/";
        }
    }

    private boolean doesNotEndWithWildcardPath()
    {
        return !this.path.endsWith("/") && !this.path.endsWith("*");
    }

    public String getPath()
    {
        return path;
    }

    public MethodRequestMatcher getMethodRequestMatcher()
    {
        return methodRequestMatcher;
    }

    public boolean matches(HttpRequest request)
    {
        return methodRequestMatcher.matches(request);
    }

    @Override
    public String toString()
    {
        return "ListenerRequestMatcher{" +
               "path='" + path + '\'' +
               ", methodRequestMatcher=" + methodRequestMatcher +
               '}';
    }
}
