/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.exception;

import org.mule.api.annotation.NoImplement;

/**
 * This is used to provide a rollback method in order to achieve atomic message delivery without relying on JTA transactions, The
 * exact behavior of this method will depend on the transport, e.g. it may send a negative ack, reset a semaphore, put the
 * resource back in its original state/location, etc.
 */
@NoImplement
public interface RollbackSourceCallback {

  void rollback();
}


