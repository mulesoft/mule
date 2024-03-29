/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import org.mule.api.annotation.NoImplement;

/**
 * A generic contract for any component that is considered as declaring point of different extensions components (Sources,
 * Connection Providers and Operation Containers)
 * <p>
 * The ones considered as declaring points are the Extensions and Extension's configurations.
 *
 * @since 4.0
 */
@NoImplement
public interface ComponentElement
    extends WithMessageSources, WithOperationContainers, WithFunctionContainers, WithConnectionProviders,
    ParameterizableTypeElement {

}
