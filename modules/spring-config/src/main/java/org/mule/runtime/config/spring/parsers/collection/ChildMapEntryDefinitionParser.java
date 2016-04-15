/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.collection;

import org.mule.config.spring.parsers.generic.AutoIdUtils;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;

import org.w3c.dom.Element;

/**
 * This definition parser only works for maps in which each entry in the map
 * is represented in the XML by a tag with name 'entry'.
 *
 * For a more customizable implementation check {@link GenericChildMapDefinitionParser}
 */
public class ChildMapEntryDefinitionParser extends ChildDefinitionParser
{

    public static final String KEY = "key";
    public static final String VALUE = "value";

    public ChildMapEntryDefinitionParser(String mapName)
    {
        super(mapName, KeyValuePair.class);
    }

    public ChildMapEntryDefinitionParser(String mapName, String keyName, String valueName)
    {
        this(mapName);
        addAlias(keyName, KEY);
        addAlias(valueName, VALUE);
    }
    
    @Override
    public String getBeanName(Element e)
    {
        // Use parent bean name always given map entry is part of map in parent
        String parentId = getParentBeanName(e);
        if (!parentId.startsWith("."))
        {
            parentId = "." + parentId;
        }
        return AutoIdUtils.uniqueValue(parentId + ":" + e.getLocalName());
    }

    public static class KeyValuePair
    {
        private String key;
        private Object value;

        public KeyValuePair()
        {
            super();
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
