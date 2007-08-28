/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.assembly;

import java.util.Map;

/**
 * This collects together various constraints/rewrites that can be applied to attributes.  It
 * was extracted from AbstractMuleBeanDefinitionParser and should be used as a delegate
 * (see that class for an example).
 */
public interface PropertyConfiguration
{

    void addReference(String propertyName);

    void addMapping(String propertyName, Map mappings);

    void addMapping(String propertyName, String mappings);

    void addAlias(String alias, String propertyName);

    void addCollection(String propertyName);

    void addIgnored(String propertyName);

    String getAttributeMapping(String alias);

    boolean isCollection(String propertyName);

    boolean isIgnored(String propertyName);

    /**
     * A property can be explicitly registered as a bean reference via registerBeanReference()
     * or it can simply use the "-ref" suffix.
     * @param attributeName true if the name appears to correspond to a reference
     */
    boolean isBeanReference(String attributeName);
    
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
    String translateName(String oldName);

    String translateValue(String name, String value);

}
