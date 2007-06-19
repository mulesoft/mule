/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.core.Conventions;
import org.springframework.util.StringUtils;

/**
 * This collects together various constraints/rewrites that can be applied to attributes.  It
 * was extracted from AbstractMuleBeanDefinitionParser and should be used as a delegate
 * (see that class for an example).
 */
public class PropertyToolkit
{

    public static final String ATTRIBUTE_REF_SUFFIX = "-ref";
    protected List beanReferences = new ArrayList();
    protected Properties attributeMappings = new Properties();
    protected Map valueMappings = new HashMap();
    protected Set collections = new HashSet();
    protected Set ignored = new HashSet();

    public void registerBeanReference(String propertyName)
    {
        beanReferences.add(propertyName);
    }

    public void registerValueMapping(ValueMap mapping)
    {
        valueMappings.put(mapping.getPropertyName(), mapping);
    }

    public void registerValueMapping(String propertyName, Map mappings)
    {
        valueMappings.put(propertyName, new ValueMap(propertyName, mappings));
    }

    public void registerValueMapping(String propertyName, String mappings)
    {
        valueMappings.put(propertyName, new ValueMap(propertyName, mappings));
    }

    public void registerAttributeMapping(String alias, String propertyName)
    {
        attributeMappings.put(alias, propertyName);
    }

    public void registerCollection(String propertyName)
    {
        collections.add(propertyName);
    }

    public void registerIgnored(String propertyName)
    {
        ignored.add(propertyName);
    }

    public String getAttributeMapping(String alias)
    {
        return attributeMappings.getProperty(alias, alias);
    }

    public boolean isCollection(String propertyName)
    {
        return collections.contains(propertyName);
    }

    public boolean isIgnored(String propertyName)
    {
        return ignored.contains(propertyName);
    }

    /**
     * A property can be explicitly registered as a bean reference via registerBeanReference()
     * or it can simply use the "-ref" suffix.
     */
    public boolean isBeanReference(String attributeName)
    {
        return (beanReferences.contains(attributeName) || attributeName.endsWith(ATTRIBUTE_REF_SUFFIX));
    }

     /**
     * Extract a JavaBean property name from the supplied attribute name.
     * <p>The default implementation uses the {@link org.springframework.core.Conventions#attributeNameToPropertyName(String)}
     * method to perform the extraction.
     * <p>The name returned must obey the standard JavaBean property name
     * conventions. For example for a class with a setter method
     * '<code>setBingoHallFavourite(String)</code>', the name returned had
     * better be '<code>bingoHallFavourite</code>' (with that exact casing).
     *
     * @param attributeName the attribute name taken straight from the XML element being parsed; will never be <code>null</code>
     * @return the extracted JavaBean property name; must never be <code>null</code>
     */
    public String extractPropertyName(String attributeName)
    {
        // Remove the bean reference suffix if any.
        attributeName = org.mule.util.StringUtils.chomp(attributeName, ATTRIBUTE_REF_SUFFIX);
        // Map to the real property name if necessary.
        attributeName = getAttributeMapping(attributeName);
        // JavaBeans property convention.
        return Conventions.attributeNameToPropertyName(attributeName);
    }

    public String extractPropertyValue(String attributeName, String attributeValue)
    {
        ValueMap vm = (ValueMap) valueMappings.get(attributeName);
        if(vm!=null)
        {
            return vm.getValue(attributeValue).toString();
        }
        else {
            return attributeValue;
        }
    }


    public static class ValueMap
    {
        private String propertyName;
        private Map mappings;

        public ValueMap(String propertyName, String mappingsString)
        {
            this.propertyName = propertyName;
            mappings = new HashMap();

            String[] values = StringUtils.tokenizeToStringArray(mappingsString, ",");
            for (int i = 0; i < values.length; i++)
            {
                String value = values[i];
                int x = value.indexOf("=");
                if(x==-1)
                {
                    throw new IllegalArgumentException("Mappings string not properly defined: " + mappingsString);
                }
                mappings.put(value.substring(0, x), value.substring(x+1));
            }

        }

        public ValueMap(String propertyName, Map mappings)
        {
            this.propertyName = propertyName;
            this.mappings = mappings;
        }

        public String getPropertyName()
        {
            return propertyName;
        }

        public Object getValue(Object key)
        {
            return mappings.get(key);
        }
    }

}
