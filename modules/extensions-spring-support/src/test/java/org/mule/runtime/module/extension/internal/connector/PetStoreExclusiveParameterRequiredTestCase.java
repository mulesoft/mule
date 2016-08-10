/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connector;

import org.mule.functional.junit4.InvalidExtensionConfigTestCase;
import org.mule.test.petstore.extension.PetStoreConnector;

public class PetStoreExclusiveParameterRequiredTestCase extends InvalidExtensionConfigTestCase
{
    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {PetStoreConnector.class};
    }

    @Override
    protected String getConfigFile()
    {
        return "petstore-exclusive-required-parameter.xml";
    }
}
