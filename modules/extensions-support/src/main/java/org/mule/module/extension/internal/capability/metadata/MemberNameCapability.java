/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.capability.metadata;

import org.mule.api.NamedObject;
import org.mule.extension.annotations.Parameter;
import org.mule.extension.introspection.Capable;
import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.Described;

/**
 * A capability to link a {@link Capable} object which
 * is also an instance of {@link Described} or {@link NamedObject}
 * to the actual member in which it was defined.
 *
 * The most common use case for this metadata capability is to support the
 * {@link Parameter#alias()} attribute. For example, consider a {@link Configuration}
 * parameter which is obtained through inspecting fields in a class. This capability
 * allows for the introspection model to list the parameter by a given alias, while
 * this parameter still provides the real name of the field which is going to be needed
 * for further operations
 *
 * @since 3.7.0
 */
public final class MemberNameCapability
{

    private final String name;

    public MemberNameCapability(String name)
    {
        this.name = name;
    }

    /**
     * The name of the member in which an aliased
     * object is defined
     *
     * @return a {@link String}
     */
    public String getName()
    {
        return name;
    }
}
