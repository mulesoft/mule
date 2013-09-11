/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
