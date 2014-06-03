/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.introspection.api;

/**
 * A {@link org.mule.extensions.introspection.api.MuleExtensionType} that defines a
 * module. A module is an extension which simply adds messages processors that accomplish coherent tasks
 * on a consistent domain. Modules do not necessarily imply a connection to an external system (stateful or not)
 *
 * @since 1.0
 */
class ModuleExtensionType extends MuleExtensionType
{

    protected static final ModuleExtensionType INSTANCE = new ModuleExtensionType();

    private ModuleExtensionType()
    {
    }
}
