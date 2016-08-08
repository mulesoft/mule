/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer.model;

import java.util.List;

/**
 * A contract for an element to be considered as a Extension
 *
 * @since 4.0
 */
public interface ExtensionElement extends TypeBasedComponent, ConfigurationElement
{

    /**
     * {@inheritDoc}
     */
    List<ConfigurationElement> getConfigurations();
}
