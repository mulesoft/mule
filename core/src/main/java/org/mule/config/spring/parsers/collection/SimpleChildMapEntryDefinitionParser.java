/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.collection;

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;

public class SimpleChildMapEntryDefinitionParser extends ChildDefinitionParser
{

    public SimpleChildMapEntryDefinitionParser(String mapName, String keyName, String valueName)
    {
        super(mapName, ChildMapEntryDefinitionParser.KeyValuePair.class);
        addAlias(keyName, "key");
        addAlias(valueName, "value");
    }

}
