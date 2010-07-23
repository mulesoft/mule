/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;


public class TransformerDefinitionParser extends MessageProcessorDefinitionParser
{
    public static final String TRANSFORMER = "transformer";
    public static final String RESPONSE_TRANSFORMER = "responseTransformer";
    public static final String RESPONSE_TRANSFORMERS = "response-transformers";

    public TransformerDefinitionParser(Class clazz)
    {
        super(clazz);
    }

    /**
     * For custom transformers
     */
    public TransformerDefinitionParser()
    {
        super();
    }

}
