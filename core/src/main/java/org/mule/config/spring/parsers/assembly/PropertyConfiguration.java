/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.assembly;

import org.mule.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.Conventions;
import org.springframework.util.StringUtils;

/**
 * This collects together various constraints/rewrites that can be applied to attributes.  It
 * was extracted from AbstractMuleBeanDefinitionParser and should be used as a delegate
 * (see that class for an example).
 */
public class PropertyConfiguration
{

    public static final String ATTRIBUTE_REF_SUFFIX = "-ref";
    private List references = new ArrayList();
    private Properties nameMappings = new Properties();
    private Map valueMappings = new HashMap();
    private Set collections = new HashSet();
    private Set ignored = new HashSet();
    private Log logger = LogFactory.getLog(getClass());

    public void addReference(String propertyName)
    {
        references.add(propertyName);
    }

    public void addMapping(ValueMap mapping)
    {
        valueMappings.put(mapping.getPropertyName(), mapping);
    }

    public void addMapping(String propertyName, Map mappings)
    {
        valueMappings.put(propertyName, new ValueMap(propertyName, mappings));
    }

    public void addMapping(String propertyName, String mappings)
    {
        valueMappings.put(propertyName, new ValueMap(propertyName, mappings));
    }

    public void addAlias(String alias, String propertyName)
    {
        nameMappings.put(alias, propertyName);
    }

    public void addCollection(String propertyName)
    {
        collections.add(propertyName);
    }

    public void addIgnored(String propertyName)
    {
        ignored.add(propertyName);
    }

    public String getAttributeMapping(String alias)
    {
        return nameMappings.getProperty(alias, alias);
    }

    public boolean isCollection(String propertyName)
    {
        return collections.contains(propertyName);
    }

    public boolean isIgnored(String propertyName)
    {
        return ignored.contains(propertyName);
    }

    protected String bestGuessName(String oldName, String className)
    {
        String newName = translateName(oldName);
        if (! methodExists(className, newName))
        {
            String plural = newName + "s";
            if (methodExists(className, plural))
            {
                // this lets us avoid setting addCollection in the majority of cases
                addCollection(plural);
                return plural;
            }
        }
        return newName;
    }

    protected boolean methodExists(String className, String newName)
    {
        try
        {
            // is there a better way than this?!
            // BeanWrapperImpl instantiates an instance, which we don't want.
            // if there really is no better way, i guess it should go in
            // class or bean utils.
            Class clazz = ClassUtils.getClass(className);
            Method[] methods = clazz.getMethods();
            String setter = "set" + newName;
            for (int i = 0; i < methods.length; ++i)
            {
                if (methods[i].getName().equalsIgnoreCase(setter))
                {
                    return true;
                }
            }
        }
        catch (Exception e)
        {
            logger.debug("Could not access bean class " + className, e);
        }
        return false;
    }

    /**
     * A property can be explicitly registered as a bean reference via registerBeanReference()
     * or it can simply use the "-ref" suffix.
     * @param attributeName true if the name appears to correspond to a reference
     */
    public boolean isBeanReference(String attributeName)
    {
        return (references.contains(attributeName) || attributeName.endsWith(ATTRIBUTE_REF_SUFFIX));
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
     * @param oldName the attribute name taken straight from the XML element being parsed; will never be <code>null</code>
     * @return the extracted JavaBean property name; must never be <code>null</code>
     */
    public String translateName(String oldName)
    {
        // Remove the bean reference suffix if any.
        String name = org.mule.util.StringUtils.chomp(oldName, ATTRIBUTE_REF_SUFFIX);
        // Map to the real property name if necessary.
        name = getAttributeMapping(name);
        // JavaBeans property convention.
        name = Conventions.attributeNameToPropertyName(name);
        if (!StringUtils.hasText(name))
        {
            throw new IllegalStateException("Illegal property name for " + oldName + ": cannot be null or empty.");
        }
        return name;
    }

    public String translateValue(String name, String value)
    {
        ValueMap vm = (ValueMap) valueMappings.get(name);
        if(vm!=null)
        {
            return vm.getValue(value).toString();
        }
        else {
            return value;
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
