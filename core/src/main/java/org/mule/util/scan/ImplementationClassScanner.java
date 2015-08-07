/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.scan;

import org.mule.util.scan.annotations.ClosableClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.EmptyVisitor;

/**
 * Will determine if the class provide extends and thus is assignable from the implementation class provided.
 *
 * @deprecated: As ASM 3.3.1 is not fully compliant with Java 8, this class has been deprecated, however you can still use it under Java 7.
 */
@Deprecated
public class ImplementationClassScanner extends EmptyVisitor implements ClassScanner
{
    private Class implementationClass;

    private boolean match;

    private String className;

    private ClassLoader classLoader;

    public ImplementationClassScanner(Class implementationClass)
    {
        this(implementationClass, Thread.currentThread().getContextClassLoader());
    }

    public ImplementationClassScanner(Class implementationClass, ClassLoader classLoader)
    {
        if(implementationClass.isInterface())
        {
            throw new IllegalArgumentException("The class need to be an implementation not an interface");
        }
        this.implementationClass = implementationClass;
        this.classLoader = classLoader;
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
                URL classURL = getClassURL(superName);
                InputStream classStream = classURL.openStream();
                ClassReader r = new ClosableClassReader(classStream);
                
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

    public URL getClassURL(String className)
    {
        String resource = className.replace(".", "/") + ".class";
        return classLoader.getResource(resource);
    }
}
