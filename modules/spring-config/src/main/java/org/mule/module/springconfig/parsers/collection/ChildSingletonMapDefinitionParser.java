/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers.collection;

import org.mule.module.springconfig.parsers.assembly.MapEntryCombiner;
import org.mule.module.springconfig.parsers.generic.ChildDefinitionParser;

public class ChildSingletonMapDefinitionParser extends ChildDefinitionParser
{
    public ChildSingletonMapDefinitionParser(String setterMethod)
    {
        super(setterMethod, MapEntryCombiner.class);
    }
}
