/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.privileged.extension;

import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.property.CompletableComponentExecutorModelProperty;

public class TestNonBlockingOperationDeclarationEnricher implements DeclarationEnricher {

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    extensionLoadingContext.getExtensionDeclarer().getDeclaration().getOperations().stream()
        .filter(o -> o.getName().equals("doSomethingAsync"))
        .findFirst()
        .ifPresent(operation -> operation.addModelProperty(
                                                           new CompletableComponentExecutorModelProperty((model,
                                                                                                          params) -> new PrivilegedNonBlockingComponentExecutor())));
  }
}
