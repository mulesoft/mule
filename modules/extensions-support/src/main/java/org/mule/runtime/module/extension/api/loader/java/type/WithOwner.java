/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
