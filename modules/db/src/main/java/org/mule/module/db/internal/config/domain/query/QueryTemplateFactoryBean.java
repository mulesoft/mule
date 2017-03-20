/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.domain.query;

import static org.mule.module.db.internal.util.ValueUtils.convertsNullStringToNull;
import org.mule.module.db.internal.domain.param.DefaultInputQueryParam;
import org.mule.module.db.internal.domain.param.QueryParam;
import org.mule.module.db.internal.domain.query.QueryParamValue;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.param.InputQueryParam;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.config.AbstractFactoryBean;

public class QueryTemplateFactoryBean extends AbstractFactoryBean<QueryTemplate>
{

    private final QueryTemplate queryTemplate;
    private final List<QueryParamValue> params;

    public QueryTemplateFactoryBean(QueryTemplate queryTemplate, List<QueryParamValue> params)
    {
        this.queryTemplate = queryTemplate;

        if (params != null)
        {
            this.params = params;
        }
        else
        {
            this.params = Collections.emptyList();
        }
    }

    @Override
    public Class<?> getObjectType()
    {
        return QueryTemplate.class;
    }

    @Override
    protected QueryTemplate createInstance() throws Exception
    {
        // No need to processStatement a new query definition
        if (params.isEmpty())
        {
            return queryTemplate;
        }

        List<QueryParam> paramDefinitions = new LinkedList<QueryParam>();

        boolean usesNameParamOverride = usesNamedParamOverride();

        if (usesNameParamOverride)
        {
            processNameParamOverride(paramDefinitions);
        }

        return new QueryTemplate(queryTemplate.getSqlText(), queryTemplate.getType(), paramDefinitions);
    }

    private void processNameParamOverride(List<QueryParam> paramDefinitions)
    {
        for (InputQueryParam param : queryTemplate.getInputParams())
        {
            Object value;

            QueryParamValue queryParamValue = getOverriddenParam(param.getName());
            if (queryParamValue != null)
            {
                value = queryParamValue.getValue();
            }
            else
            {
                value = param.getValue();
            }

            DefaultInputQueryParam newParam = new DefaultInputQueryParam(param.getIndex(), param.getType(), convertsNullStringToNull(value), param.getName());

            paramDefinitions.add(newParam);
        }
    }

    private QueryParamValue getOverriddenParam(String name)
    {
        for (QueryParamValue param : params)
        {
            if (name.equals(param.getName()))
            {
                return param;
            }
        }

        return null;
    }

    private boolean usesNamedParamOverride()
    {
        boolean result = false;
        for (QueryParamValue param : params)
        {

            if (!(param.getName() == null || "".equals(param.getName())))
            {
                result = true;
                break;
            }
        }

        return result;
    }
}
