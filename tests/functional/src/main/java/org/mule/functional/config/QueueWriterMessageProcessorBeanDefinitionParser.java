/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.config;

import org.mule.runtime.config.spring.parsers.generic.AutoIdUtils;
import org.mule.runtime.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.functional.client.QueueWriterMessageProcessor;

import org.w3c.dom.Element;

/**
 * Parses bean definitions for {@link QueueWriterMessageProcessor}
 */
public class QueueWriterMessageProcessorBeanDefinitionParser extends ChildDefinitionParser {

  public QueueWriterMessageProcessorBeanDefinitionParser() {
    super("messageProcessor", QueueWriterMessageProcessor.class);
  }

  @Override
  public String getBeanName(Element element) {
    return AutoIdUtils.uniqueValue("test.queue." + element.getAttribute("name"));
  }

  @Override
  protected void checkElementNameUnique(Element element) {}
}
