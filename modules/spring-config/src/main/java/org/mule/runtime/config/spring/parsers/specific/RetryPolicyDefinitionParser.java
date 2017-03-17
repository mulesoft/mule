/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.parsers.specific;

import static org.mule.runtime.config.spring.dsl.api.xml.SchemaConstants.MULE_ABSTRACT_MESSAGE_SOURCE_TYPE;
import static org.mule.runtime.config.spring.dsl.api.xml.SchemaConstants.MULE_EXTENSION_CONNECTION_PROVIDER_TYPE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONFIGURATION;
import static org.w3c.dom.TypeInfo.DERIVATION_EXTENSION;
import org.mule.runtime.config.spring.parsers.generic.OptionalChildDefinitionParser;
import org.mule.runtime.core.retry.async.AsynchronousRetryTemplate;

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
public class RetryPolicyDefinitionParser extends OptionalChildDefinitionParser {

  boolean asynchronous = false;

  public RetryPolicyDefinitionParser() {
    super("retryPolicyTemplate");
  }

  public RetryPolicyDefinitionParser(Class clazz) {
    super("retryPolicyTemplate", clazz);
  }

  @Override
  protected boolean isChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
    if (isConfigElement(element)) {
      element.setAttribute(ATTRIBUTE_ID, OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE);
      return false;
    } else if (getParentElement(element).getSchemaTypeInfo()
        .isDerivedFrom(MULE_EXTENSION_CONNECTION_PROVIDER_TYPE.getNamespaceURI(),
                       MULE_EXTENSION_CONNECTION_PROVIDER_TYPE.getLocalPart(), DERIVATION_EXTENSION)) {
      return false;
    } else if (getParentElement(element).getSchemaTypeInfo().isDerivedFrom(MULE_ABSTRACT_MESSAGE_SOURCE_TYPE.getNamespaceURI(),
                                                                           MULE_ABSTRACT_MESSAGE_SOURCE_TYPE.getLocalPart(),
                                                                           DERIVATION_EXTENSION)) {
      return false;
    }
    return true;
  }

  protected boolean isConfigElement(Element element) {
    return getParentBeanName(element).equals(OBJECT_MULE_CONFIGURATION);
  }

  @Override
  protected void preProcess(Element element) {
    super.preProcess(element);

    // Is this an asynchronous retry policy?
    asynchronous = !Boolean.parseBoolean(element.getAttribute("blocking"));
    element.removeAttribute("blocking");

    // Deprecated attribute from 2.x kept for backwards-compatibility. Remove for the next major release.
    // TODO MULE-9347
    if (StringUtils.isNotEmpty(element.getAttribute("asynchronous"))) {
      asynchronous = Boolean.parseBoolean(element.getAttribute("asynchronous"));
      element.removeAttribute("asynchronous");
      return;
    }
  }

  /**
   * The BDP magic inside this method will transform this simple config:
   * <p>
   * <test:connector name="testConnector8"> <ee:reconnect blocking="false" count="5" frequency="1000"/> </test:connector>
   * <p>
   * into this equivalent config, because of the attribute asynchronous="true":
   * <p>
   * <test:connector name="testConnector8"> <spring:property name="retryPolicyTemplate">
   * <spring:bean class="org.mule.runtime.core.retry.async.AsynchronousRetryTemplate"> <spring:constructor-arg>
   * <spring:bean name="delegate" class="org.mule.runtime.core.retry.policies.SimpleRetryPolicyTemplate">
   * <spring:property name="count" value="5"/> <spring:property name="frequency" value="1000"/> </spring:bean>
   * </spring:constructor-arg> </spring:bean> </spring:property> </test:connector>
   */
  @Override
  protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
    super.parseChild(element, parserContext, builder);

    if (asynchronous) {
      // Create the AsynchronousRetryTemplate as a wrapper bean
      BeanDefinitionBuilder bdb = BeanDefinitionBuilder.genericBeanDefinition(AsynchronousRetryTemplate.class);

      if (isConfigElement(element)) {
        // rename the delegate policy
        element.removeAttribute(ATTRIBUTE_ID);
        element.setAttribute(ATTRIBUTE_ID, getBeanName(element));

        wrapDelegateRetryPolicy(element, parserContext, bdb, OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE);
      } else {
        // Generate a bean name
        String asynchWrapperName = parserContext.getReaderContext().generateBeanName(bdb.getBeanDefinition());

        wrapDelegateRetryPolicy(element, parserContext, bdb, asynchWrapperName);

        // Set the AsynchronousRetryTemplate wrapper bean on the retry policy's parent instead of the retry
        // policy itself
        // TODO MULE-9638 We can get rid of all this code once we finish migrating parsers.
        try {
          BeanDefinition parent = parserContext.getRegistry().getBeanDefinition(getParentBeanName(element));
          parent.getPropertyValues().addPropertyValue(getPropertyName(element), new RuntimeBeanReference(asynchWrapperName));
        } catch (Exception e) {
          // Continue. This happens when the parent element is parsed with the new parsing mechanism.
        }
      }
    }
  }

  protected void wrapDelegateRetryPolicy(Element element, ParserContext parserContext, BeanDefinitionBuilder bdb,
                                         String asynchWrapperName) {
    // Pass in the retry policy as a constructor argument
    bdb.addConstructorArgReference(getBeanName(element));
    // Register the new bean
    BeanDefinitionHolder holder = new BeanDefinitionHolder(bdb.getBeanDefinition(), asynchWrapperName);
    registerBeanDefinition(holder, parserContext.getRegistry());
  }
}
