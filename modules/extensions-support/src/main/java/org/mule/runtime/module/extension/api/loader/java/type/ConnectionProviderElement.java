/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import org.mule.api.annotation.NoImplement;

/**
 * A contract for an element from which an extension connection provider can be derived
 *
 * @since 4.0
 */
@NoImplement
public interface ConnectionProviderElement extends ParameterizableTypeElement, WithParameters {

}
