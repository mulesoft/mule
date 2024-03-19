/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * Mule Runtime Errors.
 *
 * @provides org.mule.runtime.ast.api.error.ErrorTypeRepositoryProvider
 *
 * @moduleGraph
 * @since 4.7
 */
module org.mule.runtime.errors {

    requires org.mule.sdk.api;
    requires org.mule.runtime.api;
    requires org.mule.runtime.metadata.model.api;
    requires org.mule.runtime.metadata.model.java;
    requires org.mule.runtime.metadata.model.message;
    requires org.mule.runtime.metadata.model.catalog;
    requires org.mule.runtime.extensions.api;
    requires org.mule.runtime.dsl.api;
    requires org.mule.runtime.artifact.ast;
    requires org.mule.runtime.artifact.ast.dependency.graph;

    requires java.inject;

    requires com.google.common;
    requires com.google.gson;
    requires org.apache.commons.lang3;

    exports org.mule.runtime.core.api.error;
    exports org.mule.runtime.config.internal.error;

    provides org.mule.runtime.ast.api.error.ErrorTypeRepositoryProvider with
            org.mule.runtime.config.internal.error.CoreErrorTypeRepositoryProvider;

}