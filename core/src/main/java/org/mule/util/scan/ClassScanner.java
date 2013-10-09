/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
