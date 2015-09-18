/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.connector.petstore;

import org.mule.api.extension.annotations.Parameter;

import java.util.List;

public abstract class AbstractPetStoreConfig implements PetStoreConfig
{

    @Parameter
    private List<String> pets;

    @Override
    public List<String> getPets()
    {
        return pets;
    }
}
