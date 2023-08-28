/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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


