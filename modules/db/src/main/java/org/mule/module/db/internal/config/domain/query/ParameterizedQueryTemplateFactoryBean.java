/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.domain.query;

import static java.lang.String.format;
import static org.apache.commons.collections.CollectionUtils.find;
import static org.mule.module.db.internal.util.ValueUtils.convertsNullStringToNull;
import org.mule.module.db.internal.domain.param.DefaultInOutQueryParam;
import org.mule.module.db.internal.domain.param.DefaultInputQueryParam;
import org.mule.module.db.internal.domain.param.DefaultOutputQueryParam;
import org.mule.module.db.internal.domain.param.InOutQueryParam;
import org.mule.module.db.internal.domain.param.InputQueryParam;
import org.mule.module.db.internal.domain.param.QueryParam;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.type.DbType;
import org.mule.module.db.internal.domain.type.UnknownDbType;
import org.mule.module.db.internal.parser.QueryTemplateParser;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections.Predicate;
import org.springframework.beans.factory.FactoryBean;

/**
 * Creates {@link QueryTemplate} for parameterized queries
 */
public class ParameterizedQueryTemplateFactoryBean implements FactoryBean<QueryTemplate>
{

    private final String sqlText;
    private final List<QueryParam> queryParams;
    private final QueryTemplateParser queryParser;

    public ParameterizedQueryTemplateFactoryBean(String sqlText, List<QueryParam> queryParams, QueryTemplateParser queryParser)
    {
        this.sqlText = sqlText;
        this.queryParams = queryParams;
        this.queryParser = queryParser;
    }

    @Override
    public QueryTemplate getObject() throws Exception
    {
        QueryTemplate queryTemplate = queryParser.parse(sqlText);

        List<QueryParam> resolvedParams = new LinkedList<QueryParam>();
        List<QueryParam> unresolvedQueryParams = new LinkedList<>(queryParams);

        for (QueryParam templateParam : queryTemplate.getParams())
        {
            QueryParam param = findOverriddenParam(templateParam.getName(), queryParams);

            if (param == null && templateParam instanceof InputQueryParam && ((InputQueryParam) templateParam).getValue() == null)
            {
                throw new IllegalArgumentException(buildNotDefinedInParamErrorMessage(templateParam.getName()));
            }
            else if (param == null)
            {
                resolvedParams.add(templateParam);
            }
            else
            {
                unresolvedQueryParams.remove(param);
                resolvedParams.add(overrideParam(templateParam, param));
            }
        }

        if (!unresolvedQueryParams.isEmpty())
        {
            throw new IllegalStateException(buildUnresolvedParamErrorMsg(unresolvedQueryParams));
        }

        return new QueryTemplate(queryTemplate.getSqlText(), queryTemplate.getType(), resolvedParams);
    }

    private String buildUnresolvedParamErrorMsg(List<QueryParam> unresolvedQueryParams)
    {
        StringBuilder errorMsgBuilder = new StringBuilder();

        for (QueryParam queryParam : unresolvedQueryParams)
        {
            if (errorMsgBuilder.length() > 0)
            {
                errorMsgBuilder.append(", ");
            }
            errorMsgBuilder.append("'").append(queryParam.getName()).append("'");
        }
        errorMsgBuilder.insert(0, "There is at least a query parameter that does not match the name of any parameter defined in the query text. Unresolved parameters: ");

        return errorMsgBuilder.toString();
    }

    private QueryParam overrideParam(QueryParam templateParam, QueryParam queryParam)
    {
        QueryParam overriddenParam;
        DbType paramType = templateParam.getType();
        if (!(queryParam.getType() instanceof UnknownDbType))
        {
           paramType = queryParam.getType();
        }

        if (queryParam instanceof InOutQueryParam)
        {
            overriddenParam = new DefaultInOutQueryParam(templateParam.getIndex(), paramType, templateParam.getName(), convertsNullStringToNull(((InOutQueryParam) queryParam).getValue()));
        }
        else if (queryParam instanceof InputQueryParam)
        {
            overriddenParam = new DefaultInputQueryParam(templateParam.getIndex(), paramType, convertsNullStringToNull(((InputQueryParam) queryParam).getValue()), templateParam.getName());
        }
        else
        {
            overriddenParam = new DefaultOutputQueryParam(templateParam.getIndex(), paramType, templateParam.getName());
        }

        return overriddenParam;
    }

    private QueryParam findOverriddenParam(final String name, List<QueryParam> queryParams)
    {
        if (name != null)
        {
            return (QueryParam) find(queryParams, new Predicate()
            {
                @Override
                public boolean evaluate(Object object)
                {
                    return name.equals(((QueryParam) object).getName());
                }
            });
        }

        return null;
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

    private String buildNotDefinedInParamErrorMessage(String name)
    {
        return format("Parameter with name '%s', used in the query text, does not match any defined query parameter name defined in the query template", name);
    }

}
