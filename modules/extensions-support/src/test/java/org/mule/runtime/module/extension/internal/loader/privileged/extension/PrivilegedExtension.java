/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.privileged.extension;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.privileged.DeclarationEnrichers;

@DeclarationEnrichers({ChangeNameDeclarationEnricher.class, TestNonBlockingOperationDeclarationEnricher.class})
@Extension(name = "privileged")
@Operations(PrivilegedOperations.class)
public class PrivilegedExtension {
}
