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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Attr;

/**
 * Bean Assembler provides a high-level interface to constructing beans.  It encapsulates all
 * the "smart" logic about collections, maps, references, etc.
 *
 * <p>A bean assembly contains a bean (the thing we are constructing), a target (where we put the
 * bean once it is ready) and appropriate configuration information (there is a configuration
 * for both bean and target, but currently they are set to the same instance by the classes that
 * use this).
 */
public interface BeanAssembler
{

    public BeanDefinitionBuilder getBean();
    public BeanDefinition getTarget();

    /**
     * Add a property defined by an attribute to the bean we are constructing.
     *
     * <p>Since an attribute value is always a string, we don't have to deal with complex types
     * here - the only issue is whether or not we have a reference.  References are detected
     * by explicit annotation or by the "-ref" at the end of an attribute name.  We do not
     * check the Spring repo to see if a name already exists since that could lead to
     * unpredictable behaviour.
     * (see {@link PropertyConfiguration})
     * @param attribute The attribute to add
     */
    void extendBean(Attr attribute);

    /**
     * Allow direct access to bean for more complex cases
     *
     * @param newName The property name to add
     * @param newValue The property value to add
     * @param isReference If true, a bean reference is added (and newValue must be a String)
     */
    void extendBean(String newName, Object newValue, boolean isReference);

    /**
     * Add a property defined by an attribute to the parent of the bean we are constructing.
     *
     * <p>This is unusual.  Normally you want {@link #extendBean(org.w3c.dom.Attr)}.
     * @param attribute The attribute to add
     */
    void extendTarget(Attr attribute);

    /**
     * Allow direct access to target for more complex cases
     *
     * @param newName The property name to add
     * @param newValue The property value to add
     * @param isReference If true, a bean reference is added (and newValue must be a String)
     */
    void extendTarget(String newName, Object newValue, boolean isReference);

    /**
     * Insert the bean we have built into the target (typically the parent bean).
     *
     * <p>This is the most complex case because the bean can have an aribtrary type.
     * @param oldName The identifying the bean (typically element name).
     */
    void insertBeanInTarget(String oldName);

    /**
     * Copy the properties from the bean we have been building into the target (typically
     * the parent bean).  In other words, the bean is a facade for the target.
     *
     * <p>This assumes that the source bean has been constructed correctly (ie the decisions about
     * what is ignored, a map, a list, etc) have already been made.   All it does (apart from a
     * direct copy) is merge collections with those on the target when necessary.
     */
    void copyBeanToTarget();

}
