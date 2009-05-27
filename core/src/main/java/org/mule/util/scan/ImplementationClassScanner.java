/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.scan;

import java.io.IOException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.EmptyVisitor;

/**
 * Will determine if the class provide extends and thus is assignable from the implementatation class provied.
 */
public class ImplementationClassScanner extends EmptyVisitor implements ClassScanner
{
    private Class implementationClass;

    private boolean match;

    private String className;

    public ImplementationClassScanner(Class implementationClass)
    {
        if(implementationClass.isInterface())
        {
            throw new IllegalArgumentException("The class need to be an implementation not an interface");
        }
        this.implementationClass = implementationClass;
    }

    public void visit(int i, int i1, String s, String s1, String superName, String[] interfaces)
    {

        if(superName==null)
        {
            return;
        }
        else if(superName.replaceAll("/",".").equals(implementationClass.getName()))
        {
            match = true;
            className = s;
        }
        else
        {
            try
            {
                ImplementationClassScanner scanner = new ImplementationClassScanner(implementationClass);
                ClassReader r = new ClassReader(superName);
                r.accept(scanner, 0);
                match = scanner.isMatch();
                className = scanner.getClassName();
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }

        }
    }

    public boolean isMatch()
    {
        return match;
    }

    public String getClassName()
    {
        return className;
    }
}