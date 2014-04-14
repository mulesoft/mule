/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.domain.query;

import org.mule.module.db.internal.config.domain.param.InOutParamDefinitionDefinitionParser;
import org.mule.module.db.internal.config.domain.param.InputParamDefinitionDefinitionParser;
import org.mule.module.db.internal.config.domain.param.InputParamValueBeanDefinitionParser;
import org.mule.module.db.internal.config.domain.param.OutputParamDefinitionDefinitionParser;
import org.mule.module.db.internal.domain.param.InputQueryParam;
import org.mule.module.db.internal.domain.param.QueryParam;
import org.mule.module.db.internal.domain.query.Query;
import org.mule.module.db.internal.domain.query.QueryParamValue;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.query.QueryType;
import org.mule.module.db.internal.parser.QueryTemplateParser;
import org.mule.module.db.internal.parser.SimpleQueryTemplateParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

public class ParameterizedQueryDefinitionParser
{

    public static final String PARAMETERIZED_QUERY = "parameterized-query";
    public static final String NAME_ATTRIBUTE = "name";
    public static final String TEMPLATE_QUERY_REF = "template-query-ref";
    public static final String DYNAMIC_QUERY = "dynamic-query";
    public static final String[] QUERY_TAG_NAMES = new String[] {PARAMETERIZED_QUERY, TEMPLATE_QUERY_REF, DYNAMIC_QUERY};
    public static final String IN_PARAM_TAG = "in-param";

    private BeanDefinition parseQuery(Element queryElement, List<Element> paramElements, ParserContext nestedCtx, BeanDefinition sqlParamResolver, BeanDefinition dbConfigResolver)
    {
        if (PARAMETERIZED_QUERY.equals(queryElement.getLocalName()))
        {
            return parseParameterizedQuery(queryElement, paramElements, nestedCtx, sqlParamResolver, dbConfigResolver);
        }
        else
        {
            if (TEMPLATE_QUERY_REF.equals(queryElement.getLocalName()))
            {
                return parseTemplateQueryRef(queryElement, paramElements, nestedCtx, sqlParamResolver, dbConfigResolver);
            }
            else if (DYNAMIC_QUERY.equals(queryElement.getLocalName()))
            {
                return parseDynamicQuery(queryElement, paramElements, nestedCtx, sqlParamResolver, dbConfigResolver);
            }
            else
            {
                throw new IllegalStateException("Element must contain an element in: " + Arrays.toString(QUERY_TAG_NAMES));
            }
        }
    }

    private BeanDefinition parseDynamicQuery(Element queryElement, List<Element> paramElements, ParserContext nestedCtx, BeanDefinition sqlParamResolver, BeanDefinition dbConfigResolver)
    {
        List<BeanDefinition> paramValues = parseParameterValues(paramElements, nestedCtx);
        String sql = queryElement.getTextContent();

        QueryTemplate queryTemplate = new QueryTemplate(sql, QueryType.DDL, Collections.<QueryParam>emptyList(), true);
        BeanDefinitionBuilder queryBuilder = BeanDefinitionBuilder.genericBeanDefinition(Query.class);
        queryBuilder.addConstructorArgValue(queryTemplate);
        queryBuilder.addConstructorArgValue(paramValues);
        BeanDefinition queryBean = queryBuilder.getBeanDefinition();

        //BeanDefinitionBuilder queryResolverFactoryBean = BeanDefinitionBuilder.genericBeanDefinition(QueryResolverFactoryBean.class);
        //queryResolverFactoryBean.addConstructorArgValue(queryBean);
        //queryResolverFactoryBean.addConstructorArgValue(sqlParamResolver);
        //queryResolverFactoryBean.addConstructorArgValue(dbConfigResolver);
        //return queryResolverFactoryBean.getBeanDefinition();

        return queryBean;
    }

    private BeanDefinition parseTemplateQueryRef(Element queryElement, List<Element> paramElements, ParserContext nestedCtx, BeanDefinition sqlParamResolver, BeanDefinition dbConfigResolver)
    {
        String queryTemplateRef = queryElement.getAttribute(NAME_ATTRIBUTE);

        List<BeanDefinition> paramValues = parseParameterValues(paramElements, nestedCtx);

        BeanDefinitionBuilder queryTemplateFactoryBean = BeanDefinitionBuilder.genericBeanDefinition(QueryTemplateFactoryBean.class);
        queryTemplateFactoryBean.addConstructorArgReference(queryTemplateRef);
        queryTemplateFactoryBean.addConstructorArgValue(paramValues);

        BeanDefinitionBuilder queryBuilder = BeanDefinitionBuilder.genericBeanDefinition(Query.class);
        queryBuilder.addConstructorArgValue(queryTemplateFactoryBean.getBeanDefinition());
        BeanDefinition queryBean = queryBuilder.getBeanDefinition();

        //BeanDefinitionBuilder queryResolverFactoryBean = BeanDefinitionBuilder.genericBeanDefinition(QueryResolverFactoryBean.class);
        //queryResolverFactoryBean.addConstructorArgValue(queryBean);
        //queryResolverFactoryBean.addConstructorArgValue(sqlParamResolver);
        //queryResolverFactoryBean.addConstructorArgValue(dbConfigResolver);
        //return queryResolverFactoryBean.getBeanDefinition();
        return queryBean;
    }

    private BeanDefinition parseParameterizedQuery(Element queryElement, List<Element> paramElements, ParserContext nestedCtx, BeanDefinition sqlParamResolver, BeanDefinition dbConfigResolver)
    {
        String sql = queryElement.getTextContent();

        QueryTemplateParser sqlParser = new SimpleQueryTemplateParser();
        QueryTemplate queryTemplate = sqlParser.parse(sql);

        if (queryTemplate.getType() == QueryType.STORE_PROCEDURE_CALL)
        {
            List<BeanDefinition> newParams = parseStoreProcedureParams(paramElements, nestedCtx);
            BeanDefinitionBuilder definitionFactoryBean = BeanDefinitionBuilder.genericBeanDefinition(QueryTemplate.class);
            definitionFactoryBean.addConstructorArgValue(queryTemplate.getSqlText());
            definitionFactoryBean.addConstructorArgValue(queryTemplate.getType());
            definitionFactoryBean.addConstructorArgValue(newParams);

            BeanDefinitionBuilder queryBuilder = BeanDefinitionBuilder.genericBeanDefinition(Query.class);
            queryBuilder.addConstructorArgValue(definitionFactoryBean.getBeanDefinition());

            BeanDefinition queryBean = queryBuilder.getBeanDefinition();

            //BeanDefinitionBuilder queryResolverFactoryBean = BeanDefinitionBuilder.genericBeanDefinition(QueryResolverFactoryBean.class);
            //queryResolverFactoryBean.addConstructorArgValue(queryBean);
            //queryResolverFactoryBean.addConstructorArgValue(sqlParamResolver);
            //queryResolverFactoryBean.addConstructorArgValue(dbConfigResolver);
            //return queryResolverFactoryBean.getBeanDefinition();
            return queryBean;
        }
        else
        {
            List<BeanDefinition> params = parseParameterValues(paramElements, nestedCtx);

            BeanDefinitionBuilder queryBuilder = BeanDefinitionBuilder.genericBeanDefinition(Query.class);
            queryBuilder.addConstructorArgValue(queryTemplate);
            if (params.size() == 0 && !queryTemplate.getParams().isEmpty())
            {
                List<QueryParamValue> paramValues = new ArrayList<QueryParamValue>();
                for (InputQueryParam inputSqlParam : queryTemplate.getInputParams())
                {
                    QueryParamValue paramValue = new QueryParamValue(inputSqlParam.getName(), inputSqlParam.getValue());

                    paramValues.add(paramValue);
                }

                queryBuilder.addConstructorArgValue(paramValues);
            }
            else
            {
                queryBuilder.addConstructorArgValue(params);
            }
            BeanDefinition queryBean = queryBuilder.getBeanDefinition();

            //BeanDefinitionBuilder queryResolverFactoryBean = BeanDefinitionBuilder.genericBeanDefinition(QueryResolverFactoryBean.class);
            //queryResolverFactoryBean.addConstructorArgValue(queryBean);
            //queryResolverFactoryBean.addConstructorArgValue(sqlParamResolver);
            //queryResolverFactoryBean.addConstructorArgValue(dbConfigResolver);
            //return queryResolverFactoryBean.getBeanDefinition();
            return queryBean;
        }
    }

    private List<BeanDefinition> parseStoreProcedureParams(List<Element> paramElements, ParserContext nestedCtx)
    {
        List<BeanDefinition> params = new ManagedList<BeanDefinition>();

        for (Element param : paramElements)
        {
            BeanDefinitionParser paramParser;
            if (IN_PARAM_TAG.equals(param.getLocalName()))
            {
                paramParser = new InputParamDefinitionDefinitionParser();
            }
            else if ("out-param".equals(param.getLocalName()))
            {
                paramParser = new OutputParamDefinitionDefinitionParser();
            }
            else if ("inout-param".equals(param.getLocalName()))
            {
                paramParser = new InOutParamDefinitionDefinitionParser();
            }
            else
            {
                throw new IllegalStateException("Unsupported param type: " + param.getLocalName());
            }

            BeanDefinition paramBean = paramParser.parse(param, nestedCtx);

            params.add(paramBean);
        }

        return params;
    }

    public static List<BeanDefinition> parseParameterValues(List<Element> paramElements, ParserContext nestedCtx)
    {
        List<BeanDefinition> params = new ManagedList<BeanDefinition>();
        for (Element param : paramElements)
        {
            BeanDefinitionParser paramParser;
            if (IN_PARAM_TAG.equals(param.getLocalName()))
            {
                paramParser = new InputParamValueBeanDefinitionParser();
            }
            else
            {
                throw new IllegalStateException("Unsupported param type: " + param.getLocalName());
            }

            BeanDefinition paramBean = paramParser.parse(param, nestedCtx);

            params.add(paramBean);
        }

        return params;
    }

    public BeanDefinition parseQuery(Element element, ParserContext nestedCtx, BeanDefinition sqlParamResolver, BeanDefinition dbConfigResolver)
    {
        List<Element> childElementsByTagName = DomUtils.getChildElementsByTagName(element, QUERY_TAG_NAMES);
        if (childElementsByTagName.size() == 0)
        {
            throw new IllegalArgumentException(String.format("Element %s must contain one of the following elements: %s", element.getTagName(), Arrays.toString(QUERY_TAG_NAMES)));
        }

        List<Element> params = DomUtils.getChildElementsByTagName(element, new String[] {IN_PARAM_TAG, "out-param", "inout-param"});

        Element queryElement = childElementsByTagName.get(0);
        element.removeChild(queryElement);

        return parseQuery(queryElement, params, nestedCtx, sqlParamResolver, dbConfigResolver);
    }
}
