/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.config;

import org.mule.runtime.config.spring.parsers.specific.TransformerMessageProcessorDefinitionParser;
import org.mule.runtime.module.xml.transformer.XsltTransformer;

public class XsltTransformerDefinitionParser extends TransformerMessageProcessorDefinitionParser {

  public XsltTransformerDefinitionParser() {
    super(XsltTransformer.class);
    addAlias("transformerFactoryClass", "xslTransformerFactory");
  }

}
