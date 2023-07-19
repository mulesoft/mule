/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import org.mule.api.annotation.NoImplement;

/**
 * A generic contract for any kind of component that could describer who is their owner
 * <p>
 * This is for informative usage
 *
 * @since 4.0
 */
@NoImplement
interface WithOwner {

  String getOwnerDescription();
}
