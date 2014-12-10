/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.w3c.dom.Element;

/**
 * Allows validating an {@link Element} prior to parsing it.
 * Implementations are to be reusable and thread-safe
 *
 * @since 3.6.0
 */
public interface ElementValidator
{

    /**
     * Validates the element and throws exception if validation failed
     *
     * @param element the {@link Element} to be validated
     * @throws Exception if the element was not valid
     */
    void validate(Element element);

}
