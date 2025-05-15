/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.event;

import org.mule.runtime.api.metadata.TypedValue;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

/**
 * @deprecated This is here just for backwards compatibility of preexisting serialized objects.
 *
 */
@Deprecated
public final class DefaultMuleSession implements MuleSession {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 3380926585676521866L;

  private Map<String, TypedValue> properties;

  // //////////////////////////
  // Serialization methods
  // //////////////////////////

  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
  }

  @SuppressWarnings("unchecked")
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
  }

}
