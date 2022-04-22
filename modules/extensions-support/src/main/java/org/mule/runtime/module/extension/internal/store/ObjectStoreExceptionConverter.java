/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.store;



import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.store.ObjectAlreadyExistsException;
import org.mule.runtime.api.store.ObjectDoesNotExistException;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreNotAvailableException;

/**
 * Converts Object Store exceptions between the SDK api {@link org.mule.sdk.api.store.ObjectStoreException} and the Mule opi
 * {@link ObjectStoreException}
 *
 * @since 4.5.0
 */
public class ObjectStoreExceptionConverter {

  /**
   * Converts the Mule object store exception {@code ObjectStoreException} into the corresponding SDK api OS exception
   * {@link org.mule.sdk.api.store.ObjectStoreException}.
   *
   * @param muleObjectStoreException the Mule api OS exception to be converted
   * @return the converted SDK api OS exception
   */
  public static org.mule.sdk.api.store.ObjectStoreException from(ObjectStoreException muleObjectStoreException) {
    checkArgument(muleObjectStoreException != null, "Cannot convert null value");
    return convertException((ObjectStoreException) muleObjectStoreException);
  }

  private static org.mule.sdk.api.store.ObjectStoreException convertException(ObjectStoreException exception) {
    if (exception instanceof ObjectStoreNotAvailableException) {
      return new org.mule.sdk.api.store.ObjectStoreNotAvailableException(exception);
    } else if (exception instanceof ObjectAlreadyExistsException) {
      return new org.mule.sdk.api.store.ObjectAlreadyExistsException(exception);
    } else if (exception instanceof ObjectDoesNotExistException) {
      return new org.mule.sdk.api.store.ObjectDoesNotExistException(exception);
    } else {
      return new org.mule.sdk.api.store.ObjectStoreException(exception);
    }
  }
}
