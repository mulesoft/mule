/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.domain.connection;

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.module.db.internal.domain.connection.DbPoolingProfile;

import org.w3c.dom.Element;

public class PoolingProfileBeanDefinitionParser extends ChildDefinitionParser
{

    public PoolingProfileBeanDefinitionParser()
    {
        super("poolingProfile");
    }

    @Override
    protected Class<?> getBeanClass(Element element)
    {
        return DbPoolingProfile.class;
    }
}
