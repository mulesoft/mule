/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.config;

import org.mule.config.i18n.CoreMessages;

import javax.xml.bind.JAXBContext;

import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * TODO
 */
public class JaxbContextFactoryBean extends AbstractFactoryBean<JAXBContext>
{
    private String packageNames;
    private String name;

    @Override
    public Class<?> getObjectType()
    {
        return JAXBContext.class;
    }

    @Override
    protected JAXBContext createInstance() throws Exception
    {
        if (packageNames == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("packageNames").getMessage());
        }
        return JAXBContext.newInstance(packageNames);
    }

    public String getPackageNames()
    {
        return packageNames;
    }

    public void setPackageNames(String packageNames)
    {
        this.packageNames = packageNames;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
