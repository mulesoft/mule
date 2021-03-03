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

    public boolean isInternalDependencyInjectedIntoFunctions() {
        return registryBean != null;
    }

}
