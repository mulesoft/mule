/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.config;

import org.mule.module.springconfig.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.module.xml.transformer.XsltTransformer;

public class XsltTransformerDefinitionParser extends MessageProcessorDefinitionParser
{
    public XsltTransformerDefinitionParser()
    {
        super(XsltTransformer.class);
        addAlias("transformerFactoryClass", "xslTransformerFactory");
    }

}
