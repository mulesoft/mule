/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.withErrorDeclaration;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

public enum WithErrorDeclarationErrors implements ErrorTypeDefinition<WithErrorDeclarationErrors> {

  MY_DECLARED_ERROR
}
