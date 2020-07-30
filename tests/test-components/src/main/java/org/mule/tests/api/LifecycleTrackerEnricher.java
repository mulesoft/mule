/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.api;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.loader.java.property.ComponentExecutorModelProperty;

import org.reactivestreams.Publisher;

public class LifecycleTrackerEnricher implements DeclarationEnricher {

    @Override
    public void enrich(ExtensionLoadingContext extensionLoadingContext) {
        extensionLoadingContext.getExtensionDeclarer().getDeclaration().getOperations().forEach(operation -> {
            if (operation.getName().equals("check")) {
                operation.addModelProperty(new ComponentExecutorModelProperty((componentModel, map) -> {
                    return new LifecycleTrackerComponentExecutor();
                }));
            }
        });
    }

    private static class LifecycleTrackerComponentExecutor implements ComponentExecutor<OperationModel> {

        @Override
        public Publisher<Object> execute(ExecutionContext<OperationModel> executionContext) {
            return null;                               
        }
    }
}
