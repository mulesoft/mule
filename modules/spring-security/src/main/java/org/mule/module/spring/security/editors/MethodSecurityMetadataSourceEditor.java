/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */

package org.mule.module.spring.security.editors;

import java.beans.PropertyEditorSupport;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.access.method.MapBasedMethodSecurityMetadataSource;



/**
 * Property editor to assist with the setup of a {@link org.springframework.security.access.method.MethodSecurityMetadataSource}.
 */
public class MethodSecurityMetadataSourceEditor extends PropertyEditorSupport 
{
    /**
     * Create a MapBasedMethodSecurityMetadataSource from configured text
     * The syntax expected is a set of Java properties, eachof the form
     *     fully-qualified-method-name=comma-separated-list-of-ConfigAttributes
     */
    public void setAsText(String text) throws IllegalArgumentException 
    {
        try
        {
            Map<String, List<ConfigAttribute>> mappings = new LinkedHashMap<String, List<ConfigAttribute>>();

            if (text != null)
            {
                text = text.trim();
                if (text.length() > 0)
                {
                    Properties props = new Properties();
                    props.load(new StringReader(text));
                    for (Map.Entry entry : props.entrySet())
                    {
                        String methodName = (String) entry.getKey();
                        List<ConfigAttribute> cfgAttrs = new ArrayList<ConfigAttribute>();
                        String atrs = (String) entry.getValue();
                        String[] attrArray = atrs.split(",");
                        for (String attr : attrArray)
                        {
                            attr = attr.trim();
                            if (attr.length() > 0)
                            {
                                cfgAttrs.add(new SecurityConfig(attr));
                            }
                        }
                        mappings.put(methodName, cfgAttrs);
                    }
                }
                setValue(new MapBasedMethodSecurityMetadataSource(mappings));
            }
        }
        catch(Exception ex)
        {
            throw new IllegalArgumentException(MessageFormat.format("Error parsing {0}", text), ex);
        }
    }
}
