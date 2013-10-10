/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
