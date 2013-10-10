/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.scan;

import org.mule.util.scan.annotations.ClosableClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

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
