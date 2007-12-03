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

public class ChildMapEntryDefinitionParser extends ChildDefinitionParser
{

    public static final String KEY = "key";
    public static final String VALUE = "value";

    public ChildMapEntryDefinitionParser(String mapName, String keyName, String valueName)
    {
        super(mapName, KeyValuePair.class);
        addAlias(keyName, KEY);
        addAlias(valueName, VALUE);
    }

    public static class KeyValuePair
    {
        private String key;
        private Object value;

        public KeyValuePair()
        {
        }

        public KeyValuePair(String key, Object value)
        {
            this.key = key;
            this.value = value;
        }

        public String getKey()
        {
            return key;
        }

        public Object getValue()
        {
            return value;
        }

        public void setKey(String key)
        {
            this.key = key;
        }

        public void setValue(Object value)
        {
            this.value = value;
        }

    }
}
