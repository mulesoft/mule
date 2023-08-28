/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.policyIsolation.internal;

import javax.inject.Inject;

public class PolicyDependencyInjectionFunctions {

    @Inject
    private InternalRegistryBean registryBean;

    // The function cannot be invoked because the EL service is mocked (see MockExpressionLanguageFactoryServiceProvider),
    // but we need at least one declared function in order to trigger the dependency injection.
    public boolean isInternalDependencyInjectedIntoFunctions() {
        return registryBean != null;
    }

}
