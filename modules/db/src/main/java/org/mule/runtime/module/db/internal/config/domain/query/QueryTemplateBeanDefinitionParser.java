/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.domain.query;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.module.db.internal.config.domain.param.InputParamDefinitionDefinitionParser;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.parser.SimpleQueryTemplateParser;
import org.mule.module.db.internal.util.DefaultFileReader;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class QueryTemplateBeanDefinitionParser extends AbstractMuleBeanDefinitionParser
{

    public static final String PARAMETERIZED_QUERY = "parameterized-query";
    public static final String DYNAMIC_QUERY = "dynamic-query";
    public static final String TEMPLATE_QUERY_REF = "template-query-ref";
    public static final String IN_PARAM_ELEMENT = "in-param";
    public static final String FILE_ATTRIBUTE = "file";
    public static final String[] QUERY_DEFINITION_ELEMENTS = new String[] {PARAMETERIZED_QUERY, DYNAMIC_QUERY, TEMPLATE_QUERY_REF};

    @Override
    protected Class<?> getBeanClass(Element element)
    {
        return QueryTemplate.class;
    }

    @Override
    protected boolean isSingleton()
    {
        return true;
    }

    @Override
    protected void doParse(Element element, ParserContext context, BeanDefinitionBuilder builder)
    {
        builder.setScope(isSingleton() ? BeanDefinition.SCOPE_SINGLETON : BeanDefinition.SCOPE_PROTOTYPE);

        // We want any parsing to occur as a child of this tag so we need to make
        // a new one that has this as it's owner/parent
        ParserContext nestedCtx = new ParserContext(context.getReaderContext(), context.getDelegate(), builder.getBeanDefinition());

        Element sqlElem = DomUtils.getChildElementByTagName(element, PARAMETERIZED_QUERY);
        if (sqlElem != null)
        {
            parseParameterizedQuery(element, builder, nestedCtx, sqlElem);
        }
        else
        {
            sqlElem = DomUtils.getChildElementByTagName(element, DYNAMIC_QUERY);

            if (sqlElem != null)
            {
                parseDynamicQuery(element, builder, sqlElem);
            }
            else
            {
                sqlElem = DomUtils.getChildElementByTagName(element, TEMPLATE_QUERY_REF);
                if (sqlElem != null)
                {
                    parseQueryTemplateRef(element, builder, nestedCtx, sqlElem);
                }
                else
                {
                    throw new IllegalArgumentException("Template must contain one of the following elements: " + Arrays.toString(QUERY_DEFINITION_ELEMENTS));
                }
            }
        }
    }

    private void parseDynamicQuery(Element element, BeanDefinitionBuilder builder, Element sqlElem)
    {
        BeanDefinitionBuilder queryTemplateFactory = BeanDefinitionBuilder.genericBeanDefinition(DynamicQueryTemplateFactoryBean.class);
        queryTemplateFactory.addConstructorArgValue(sqlElem.getTextContent());

        builder.addConstructorArgValue(queryTemplateFactory.getBeanDefinition());

        element.removeChild(sqlElem);
    }

    private void parseQueryTemplateRef(Element element, BeanDefinitionBuilder builder, ParserContext nestedCtx, Element template)
    {
        String queryTemplateRef = template.getAttribute("name");
        element.removeChild(template);

        List<Element> params = DomUtils.getChildElementsByTagName(element, IN_PARAM_ELEMENT);
        List<BeanDefinition> paramList = QueryDefinitionParser.parseOverriddenTemplateParameters(params, nestedCtx);

        BeanDefinitionBuilder queryTemplateBuilder = BeanDefinitionBuilder.genericBeanDefinition(QueryTemplateFactoryBean.class);
        queryTemplateBuilder.addConstructorArgReference(queryTemplateRef);
        queryTemplateBuilder.addConstructorArgValue(paramList);
        builder.addConstructorArgValue(queryTemplateBuilder.getBeanDefinition());
    }

    private void parseParameterizedQuery(Element element, BeanDefinitionBuilder builder, ParserContext nestedCtx, Element sqlElem)
    {
        boolean hasFileAttribute = sqlElem.hasAttribute(FILE_ATTRIBUTE);
        boolean hasTextContent = !element.getTextContent().trim().isEmpty();

        if (hasFileAttribute && hasTextContent)
        {
            throw new IllegalArgumentException(String.format("Element %s cannot contain attribute file and text content simultaneously", element.getTagName()));
        }

        BeanDefinitionBuilder queryTemplateFactory = BeanDefinitionBuilder.genericBeanDefinition(ParameterizedQueryTemplateFactoryBean.class);

        if (hasFileAttribute)
        {
            String fileName = sqlElem.getAttribute(FILE_ATTRIBUTE);
            BeanDefinitionBuilder queryFileBuilder = BeanDefinitionBuilder.genericBeanDefinition(QueryFileFactoryBean.class);
            queryFileBuilder.addConstructorArgValue(fileName);
            queryFileBuilder.addConstructorArgValue(new DefaultFileReader());
            queryTemplateFactory.addConstructorArgValue(queryFileBuilder.getBeanDefinition());
        }
        else
        {
            Node node = sqlElem.getFirstChild();

            String sqlText;
            if (node.getNextSibling() != null && node.getNextSibling().getNodeType() == Node.CDATA_SECTION_NODE)
            {
                sqlText = node.getNextSibling().getNodeValue();
            }
            else
            {
                sqlText = node.getNodeValue();
            }
            queryTemplateFactory.addConstructorArgValue(sqlText);
        }
        element.removeChild(sqlElem);

        List<Object> params = new ManagedList<Object>();
        List<Element> childElementsByTagName = DomUtils.getChildElementsByTagName(element, IN_PARAM_ELEMENT);

        for (Element param : childElementsByTagName)
        {
            BeanDefinition paramBean = parseParameter(nestedCtx, param);

            params.add(paramBean);
        }

        queryTemplateFactory.addConstructorArgValue(params);
        queryTemplateFactory.addConstructorArgValue(new SimpleQueryTemplateParser());

        builder.addConstructorArgValue(queryTemplateFactory.getBeanDefinition());
    }

    private BeanDefinition parseParameter(ParserContext nestedCtx, Element param)
    {
        BeanDefinitionParser paramParser = new InputParamDefinitionDefinitionParser();
        return paramParser.parse(param, nestedCtx);
    }

}
