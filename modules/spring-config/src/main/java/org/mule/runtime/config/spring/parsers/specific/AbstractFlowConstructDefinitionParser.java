/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.parsers.specific;

import static org.mule.runtime.core.util.StringUtils.isBlank;
import org.mule.runtime.config.spring.parsers.AbstractMuleBeanDefinitionParser;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public abstract class AbstractFlowConstructDefinitionParser extends AbstractMuleBeanDefinitionParser {

  private static final String ABSTRACT_ATTRIBUTE = "abstract";
  private static final String PARENT_ATTRIBUTE = "parent";

  protected static final String ENDPOINT_REF_ATTRIBUTE = "endpoint-ref";
  protected static final String ADDRESS_ATTRIBUTE = "address";

  protected static final String INBOUND_ADDRESS_ATTRIBUTE = "inboundAddress";
  protected static final String INBOUND_ENDPOINT_REF_ATTRIBUTE = "inboundEndpoint-ref";
  protected static final String INBOUND_ENDPOINT_CHILD = "inbound-endpoint";

  protected static final String OUTBOUND_ADDRESS_ATTRIBUTE = "outboundAddress";
  protected static final String OUTBOUND_ENDPOINT_REF_ATTRIBUTE = "outboundEndpoint-ref";
  protected static final String OUTBOUND_ENDPOINT_CHILD = "outboundEndpoint";

  protected static final String TRANSFORMER_REFS_ATTRIBUTE = "transformer-refs";
  protected static final String RESPONSE_TRANSFORMER_REFS_ATTRIBUTE = "responseTransformer-refs";

  @Override
  protected BeanDefinitionBuilder createBeanDefinitionBuilder(Element element, Class<?> beanClass) {
    return BeanDefinitionBuilder.genericBeanDefinition(beanClass);
  }

  @Override
  protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
    builder.setScope(BeanDefinition.SCOPE_SINGLETON);
    handleAbstractAttribute(element, builder);
    handleParentAttribute(element, builder);
    super.doParse(element, parserContext, builder);
  }

  private void handleParentAttribute(Element element, BeanDefinitionBuilder builder) {
    final String parentAttribute = element.getAttribute(PARENT_ATTRIBUTE);
    if (!isBlank(parentAttribute)) {
      builder.setParentName(parentAttribute);
    }
    element.removeAttribute(PARENT_ATTRIBUTE);
  }

  private void handleAbstractAttribute(Element element, BeanDefinitionBuilder builder) {
    final String abstractAttribute = element.getAttribute(ABSTRACT_ATTRIBUTE);
    if (!isBlank(abstractAttribute)) {
      builder.setAbstract(Boolean.parseBoolean(abstractAttribute));
    }
    element.removeAttribute(ABSTRACT_ATTRIBUTE);
  }
}
