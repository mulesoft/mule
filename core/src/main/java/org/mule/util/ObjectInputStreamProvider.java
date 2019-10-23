/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

public interface ObjectInputStreamProvider
{
    ObjectInputStream get(ClassLoader classLoader, InputStream inputStream) throws IOException;
}
