/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.api.config.MuleProperties;
import org.mule.config.spring.parsers.generic.OptionalChildDefinitionParser;
import org.mule.retry.async.AsynchronousRetryTemplate;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Allows retry policies to be children of connector elements <i>or</i> the <mule-configuration> element.
 */
public class RetryPolicyDefinitionParser extends OptionalChildDefinitionParser
{
    boolean asynchronous = false;

    public RetryPolicyDefinitionParser()
    {
        super("retryPolicyTemplate");
    }

    public RetryPolicyDefinitionParser(Class clazz)
    {
        super("retryPolicyTemplate", clazz);
    }

    @Override
    protected boolean isChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        if (getParentBeanName(element).equals(MuleProperties.OBJECT_MULE_CONFIGURATION))
        {
            element.setAttribute(ATTRIBUTE_ID, MuleProperties.OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE);
            return false;
        }
        else
        {
            return true;
        }
    }

    @Override
    protected void preProcess(Element element)
    {
        super.preProcess(element);

        // Is this an asynchronous retry policy?
        asynchronous = !Boolean.parseBoolean(element.getAttribute("blocking"));
        element.removeAttribute("blocking");

        // Deprecated attribute from 2.x kept for backwards-compatibility.  Remove for the next major release.
        if (StringUtils.isNotEmpty(element.getAttribute("asynchronous")))
        {
            asynchronous = Boolean.parseBoolean(element.getAttribute("asynchronous"));
            element.removeAttribute("asynchronous");            
            return;
        }
    }

    /**
     * The BDP magic inside this method will transform this simple config:
     *
     *      <test:connector name="testConnector8">
     *          <ee:reconnect blocking="false" count="5" frequency="1000"/>
     *      </test:connector>
     *
     * into this equivalent config, because of the attribute asynchronous="true":
     *
     *      <test:connector name="testConnector8">
     *          <spring:property name="retryPolicyTemplate">
     *              <spring:bean class="org.mule.retry.async.AsynchronousRetryTemplate">
     *                  <spring:constructor-arg>
     *                      <spring:bean name="delegate" class="org.mule.retry.policies.SimpleRetryPolicyTemplate">
     *                          <spring:property name="count" value="5"/>
     *                          <spring:property name="frequency" value="1000"/>
     *                      </spring:bean>
     *                  </spring:constructor-arg>
     *              </spring:bean>
     *          </spring:property>
     *      </test:connector>
     */
    @Override
    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        super.parseChild(element, parserContext, builder);

        if (asynchronous)
        {
            // Create the AsynchronousRetryTemplate as a wrapper bean
            BeanDefinitionBuilder bdb = BeanDefinitionBuilder.genericBeanDefinition(AsynchronousRetryTemplate.class);
            // Generate a bean name
            String asynchWrapperName = parserContext.getReaderContext().generateBeanName(bdb.getBeanDefinition());
            // Pass in the retry policy as a constructor argument
            String retryPolicyName = getBeanName(element);
            bdb.addConstructorArgReference(retryPolicyName);
            // Register the new bean
            BeanDefinitionHolder holder = new BeanDefinitionHolder(bdb.getBeanDefinition(), asynchWrapperName);
            registerBeanDefinition(holder, parserContext.getRegistry());

            // Set the AsynchronousRetryTemplate wrapper bean on the retry policy's parent instead of the retry policy itself
            BeanDefinition parent = parserContext.getRegistry().getBeanDefinition(getParentBeanName(element));
            parent.getPropertyValues().addPropertyValue(getPropertyName(element), new RuntimeBeanReference(asynchWrapperName));
        }
    }
}