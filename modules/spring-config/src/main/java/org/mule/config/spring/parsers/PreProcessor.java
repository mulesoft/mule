/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers;

import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;

import org.w3c.dom.Element;

/**
 * This interface allows pre-processing of the element.
 */
public interface PreProcessor
{

    public void preProcess(PropertyConfiguration config, Element element);

}
