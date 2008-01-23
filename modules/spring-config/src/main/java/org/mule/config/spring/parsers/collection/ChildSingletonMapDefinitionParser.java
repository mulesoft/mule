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

import org.mule.config.spring.parsers.assembly.MapEntryCombiner;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;

public class ChildSingletonMapDefinitionParser extends ChildDefinitionParser
{

    public static final String KEY = "key";
    public static final String VALUE = "value";

    public ChildSingletonMapDefinitionParser(String setterMethod)
    {
        super(setterMethod, MapEntryCombiner.class);
    }

}