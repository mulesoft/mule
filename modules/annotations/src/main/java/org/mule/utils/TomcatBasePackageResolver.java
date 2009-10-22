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
 * When running Tomcat from Maven, we need to figure out the base path based on the Catalina Home set in the system
 * properties. This resolver extends {@link DefaultServletBasePackageResolver}, so it will work on Tomcat stand alone
 * and embedded.
 */
public class TomcatBasePackageResolver extends DefaultServletBasePackageResolver
{
    private String basePackage;

    public String getPackage(Resource resource)
    {
        if(this.basePackage !=null) return this.basePackage;
        basePackage = super.getPackage(resource);
        if(resource.getFilename().startsWith(basePackage))
        {
            return basePackage;
        }
        else
        {
            String catalinaHome = System.getProperty("catalina.home");
            if(catalinaHome.endsWith("/target/tomcat"))
            {
                //We're running embedded mode, best efforts to figure out
                basePackage = catalinaHome.substring(0, catalinaHome.lastIndexOf("tomcat")) + "classes/";
            }
            else
            {
                throw new IllegalStateException("Cannot find base scanning package for annotations");
            }
        }
        return basePackage;
    }
}