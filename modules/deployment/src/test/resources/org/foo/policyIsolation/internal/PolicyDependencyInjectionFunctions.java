/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo.policyIsolation.internal;

import jakarta.inject.Inject;

public class PolicyDependencyInjectionFunctions {

    @Inject
    private InternalRegistryBean registryBean;

    // The function cannot be invoked because the EL service is mocked (see MockExpressionLanguageFactoryServiceProvider),
    // but we need at least one declared function in order to trigger the dependency injection.
    public boolean isInternalDependencyInjectedIntoFunctions() {
        return registryBean != null;
    }

}
