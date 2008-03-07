/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific.tls;

import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;

public class KeyStoreDefinitionParser extends ParentDefinitionParser
{

    public KeyStoreDefinitionParser()
    {
        addAlias("path", "keyStore");
        addAlias(AbstractMuleBeanDefinitionParser.ATTRIBUTE_CLASS, "keyStoreType");
        addAlias("storePassword", "keyStorePassword");
        addAlias("algorithm", "keyManagerAlgorithm");
    }

}