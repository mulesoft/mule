/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * Mule Deployment Model Implementation Module
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.deployment.model.impl {

    //requires org.mule.runtime.api;
    requires org.mule.runtime.artifact;
    requires org.mule.runtime.artifact.activation;
    requires org.mule.runtime.artifact.declaration;
    requires org.mule.runtime.container;
    requires org.mule.runtime.core;
    requires org.mule.runtime.deployment.model;
    requires org.mule.runtime.extension.model;
    requires org.mule.runtime.extensions.api;
    requires org.mule.runtime.extensions.support;
    requires org.mule.runtime.http.policy.api;
    requires org.mule.runtime.license.api;
    requires org.mule.runtime.maven.client.api;
    requires org.mule.runtime.maven.pom.parser.api;
    requires org.mule.runtime.memory.management;
    requires org.mule.runtime.policy.api;
    requires org.mule.runtime.profiling.api;

    requires com.google.gson;

    exports org.mule.runtime.module.deployment.impl.internal.util to
        org.mule.runtime.deployment;

    exports org.mule.runtime.module.deployment.impl.internal.artifact to
        org.mule.runtime.deployment;

    exports org.mule.runtime.module.deployment.impl.internal.application to
        org.mule.runtime.deployment;

    exports org.mule.runtime.module.deployment.impl.internal.domain to
        org.mule.runtime.deployment;
}
