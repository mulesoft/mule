/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.utils;

import org.springframework.core.io.Resource;

/**
 * Resolves the base package to <pre>/WEB-INF/classes/</pre>. This is suitable for servlet containers.
 */
public class DefaultServletBasePackageResolver implements BasePackageResolver
{
    //the base package wich represents the root of the scanned resources/classes folder.
    public final static String DEFAULT_BASE_SCANNING_PACKAGE = "/WEB-INF/classes/";

    public String getPackage(Resource resource)
    {
        return DEFAULT_BASE_SCANNING_PACKAGE;
    }
}
