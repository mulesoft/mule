/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.domain.query;

import org.mule.module.db.internal.domain.param.QueryParam;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.query.QueryType;

import java.util.Collections;

import org.springframework.beans.factory.FactoryBean;

/**
 * Creates a {@link QueryTemplate} for a dynamic query
 */
public class DynamicQueryTemplateFactoryBean implements FactoryBean<QueryTemplate>
{

    private final String sqlText;

    public DynamicQueryTemplateFactoryBean(String sqlText)
    {
        this.sqlText = sqlText;
    }

    @Override
    public QueryTemplate getObject() throws Exception
    {
        return new QueryTemplate(sqlText, QueryType.DDL, Collections.<QueryParam>emptyList(), true);
    }

    @Override
    public Class<?> getObjectType()
    {
        return QueryTemplate.class;
    }

    @Override
    public boolean isSingleton()
    {
        return false;
    }
}
