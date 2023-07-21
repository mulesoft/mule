/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import org.mule.api.annotation.NoImplement;

/**
 * A contract for an element from which an extension configuration can be derived
 *
 * @since 4.0
 */
@NoImplement
public interface ConfigurationElement extends ParameterizableTypeElement, ComponentElement {

}
