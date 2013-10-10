/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.scan;

import org.objectweb.asm.ClassVisitor;

/**
 * This interface is used by scanners to let the {@link ClasspathScanner} know if there was a
 * match on the class and whan the class was.
 */
public interface ClassScanner extends ClassVisitor
{
    boolean isMatch();

    String getClassName();
}
