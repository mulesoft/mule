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
import org.mule.module.db.internal.domain.query.Query;
import org.mule.module.db.internal.parser.SimpleQueryTemplateParser;

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

/**
 * Parses {@link org.w3c.dom.Element} representing queries
 */
public class QueryDefinitionParser
{

    public static final String PARAMETERIZED_QUERY = "parameterized-query";
    public static final String NAME_ATTRIBUTE = "name";
    public static final String TEMPLATE_QUERY_REF = "template-query-ref";
    public static final String DYNAMIC_QUERY = "dynamic-query";
    public static final String[] QUERY_TAG_NAMES = new String[] {PARAMETERIZED_QUERY, TEMPLATE_QUERY_REF, DYNAMIC_QUERY};
    public static final String IN_PARAM_TAG = "in-param";

    private BeanDefinition parseQuery(Element queryElement, List<Element> paramElements, ParserContext nestedCtx)
    {
        if (PARAMETERIZED_QUERY.equals(queryElement.getLocalName()))
        {
            return parseParameterizedQuery(queryElement, paramElements, nestedCtx);
        }
        else if (TEMPLATE_QUERY_REF.equals(queryElement.getLocalName()))
        {
            return parseTemplateQueryRef(queryElement, paramElements, nestedCtx);
        }
        else if (DYNAMIC_QUERY.equals(queryElement.getLocalName()))
        {
            return parseDynamicQuery(queryElement);
        }
        else
        {
            throw new IllegalStateException("Element must contain an element in: " + Arrays.toString(QUERY_TAG_NAMES));
        }
    }

    private BeanDefinition parseDynamicQuery(Element queryElement)
    {
        BeanDefinitionBuilder queryTemplateFactory = BeanDefinitionBuilder.genericBeanDefinition(DynamicQueryTemplateFactoryBean.class);
        queryTemplateFactory.addConstructorArgValue(queryElement.getTextContent());

        BeanDefinitionBuilder queryBuilder = BeanDefinitionBuilder.genericBeanDefinition(Query.class);
        queryBuilder.addConstructorArgValue(queryTemplateFactory.getBeanDefinition());
        queryBuilder.addConstructorArgValue(Collections.emptyList());

        return queryBuilder.getBeanDefinition();
    }

    private BeanDefinition parseTemplateQueryRef(Element queryElement, List<Element> paramElements, ParserContext nestedCtx)
    {
        String queryTemplateRef = queryElement.getAttribute(NAME_ATTRIBUTE);

        List<BeanDefinition> paramValues = parseOverriddenTemplateParameters(paramElements, nestedCtx);

        BeanDefinitionBuilder queryTemplateFactoryBean = BeanDefinitionBuilder.genericBeanDefinition(QueryTemplateFactoryBean.class);
        queryTemplateFactoryBean.addConstructorArgReference(queryTemplateRef);
        queryTemplateFactoryBean.addConstructorArgValue(paramValues);

        BeanDefinitionBuilder queryBuilder = BeanDefinitionBuilder.genericBeanDefinition(Query.class);
        queryBuilder.addConstructorArgValue(queryTemplateFactoryBean.getBeanDefinition());

        return queryBuilder.getBeanDefinition();
    }

    private BeanDefinition parseParameterizedQuery(Element queryElement, List<Element> paramElements, ParserContext nestedCtx)
    {
        List<BeanDefinition> params = parseStoreProcedureParams(paramElements, nestedCtx);

        BeanDefinitionBuilder queryTemplateFactory = BeanDefinitionBuilder.genericBeanDefinition(ParameterizedQueryTemplateFactoryBean.class);
        queryTemplateFactory.addConstructorArgValue(queryElement.getTextContent());
        queryTemplateFactory.addConstructorArgValue(params);
        queryTemplateFactory.addConstructorArgValue(new SimpleQueryTemplateParser());

        BeanDefinitionBuilder queryBean = BeanDefinitionBuilder.genericBeanDefinition(Query.class);
        queryBean.addConstructorArgValue(queryTemplateFactory.getBeanDefinition());

        return queryBean.getBeanDefinition();
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

    public static List<BeanDefinition> parseOverriddenTemplateParameters(List<Element> paramElements, ParserContext nestedCtx)
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

    public BeanDefinition parseQuery(Element element, ParserContext nestedCtx)
    {
        List<Element> childElementsByTagName = DomUtils.getChildElementsByTagName(element, QUERY_TAG_NAMES);
        if (childElementsByTagName.size() == 0)
        {
            throw new IllegalArgumentException(String.format("Element %s must contain one of the following elements: %s", element.getTagName(), Arrays.toString(QUERY_TAG_NAMES)));
        }

        List<Element> params = DomUtils.getChildElementsByTagName(element, new String[] {IN_PARAM_TAG, "out-param", "inout-param"});

        Element queryElement = childElementsByTagName.get(0);
        element.removeChild(queryElement);

        return parseQuery(queryElement, params, nestedCtx);
    }
}
