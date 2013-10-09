/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.collection;

import org.mule.config.spring.parsers.generic.AutoIdUtils;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;

import org.w3c.dom.Element;

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
