/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.foo.withInternalDependency.internal;

import javax.inject.Inject;

public class WithInternalDependencyFunctions {

    @Inject
    private InternalRegistryBean registryBean;

    // The function cannot be invoked because the EL service is mocked (see MockExpressionLanguageFactoryServiceProvider),
    // but we need at least one declared function in order to trigger the dependency injection.
    public boolean isInternalDependencyInjectedIntoFunctions() {
        return registryBean != null;
    }

}
