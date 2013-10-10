/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

/**
 *
 */
public class DefaultMuleSharedDomainClassLoader extends MuleSharedDomainClassLoader
{

    public static final String DEFAULT_DOMAIN_NAME = "default";

    public DefaultMuleSharedDomainClassLoader(ClassLoader parent)
    {
        super(DEFAULT_DOMAIN_NAME, parent);
    }
}
