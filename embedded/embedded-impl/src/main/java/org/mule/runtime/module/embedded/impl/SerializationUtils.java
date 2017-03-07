/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.embedded.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class SerializationUtils {

  public static <T> T deserialize(byte[] object) throws IOException, ClassNotFoundException {
    try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(object)) {
      try (ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
        return (T) objectInputStream.readObject();
      }
    }
  }

}
