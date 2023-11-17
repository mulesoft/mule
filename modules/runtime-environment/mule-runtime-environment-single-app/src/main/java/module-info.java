/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

import org.mule.runtime.environment.api.DeploymentServiceProvider;
import org.mule.runtime.environment.api.RuntimeEnvironment;
import org.mule.runtime.environment.singleapp.impl.SingleAppEnvironment;
import org.mule.runtime.environment.singleapp.impl.SingleAppEnvironmentDeploymentServiceProvider;
import org.mule.runtime.module.deployment.api.DeploymentService;

/**
 * Mule Runtime Execution Single App Environment Api
 *
 * @moduleGraph
 * @since 4.7
 */
module org.mule.runtime.environment.singleapp.impl {
    requires org.mule.runtime.environment.api;
    requires org.mule.runtime.environment.singleapp.api;

    requires java.inject;
    requires org.mule.runtime.deployment;

    exports org.mule.runtime.environment.singleapp.impl;

    provides RuntimeEnvironment with
            SingleAppEnvironment;

    provides DeploymentServiceProvider with
            SingleAppEnvironmentDeploymentServiceProvider;
}