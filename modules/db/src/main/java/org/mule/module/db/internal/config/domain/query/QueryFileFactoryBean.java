/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.domain.query;

import org.mule.module.db.internal.util.FileReader;

import java.io.IOException;

import org.springframework.beans.factory.FactoryBean;

/**
 * Creates a query text from a file
 */
public class QueryFileFactoryBean implements FactoryBean<String>
{

    private final String fileName;
    private final FileReader fileReader;

    public QueryFileFactoryBean(String fileName, FileReader fileReader)
    {
        this.fileName = fileName;
        this.fileReader = fileReader;
    }

    @Override
    public String getObject() throws Exception
    {
        try
        {
            return  fileReader.getResourceAsString(fileName);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Unable to read query from file: " + fileName);
        }
    }

    @Override
    public Class<?> getObjectType()
    {
        return String.class;
    }

    @Override
    public boolean isSingleton()
    {
        return false;
    }
}
