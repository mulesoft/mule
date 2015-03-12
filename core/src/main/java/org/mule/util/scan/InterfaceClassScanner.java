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
 * Will determine if the class provide is assignable from the interface class provided.
 *
 * @deprecated: As ASM 3.3.1 is not fully compliant with Java 8, this class has been deprecated, however you can still use it under Java 7.
 */
@Deprecated
public class InterfaceClassScanner extends EmptyVisitor implements ClassScanner
{
    private Class interfaceClass;

    private boolean match;

    private String className;

    private ClassLoader classLoader;

    public InterfaceClassScanner(Class interfaceClass)
    {
        this(interfaceClass, Thread.currentThread().getContextClassLoader());
    }

    public InterfaceClassScanner(Class interfaceClass, ClassLoader classLoader)
    {
        if(!interfaceClass.isInterface())
        {
            throw new IllegalArgumentException("The class need to be an interface");
        }
        this.interfaceClass = interfaceClass;

        this.classLoader = classLoader;
    }

    public void visit(int i, int i1, String s, String s1, String superName, String[] interfaces)
    {
        if (interfaces != null && interfaces.length > 0)
        {
            for (int j = 0; j < interfaces.length; j++)
            {
                String anInterface = interfaces[j].replace("/", ".");
                if (interfaceClass.getName().equals(anInterface))
                {
                    match = true;
                    className = s;
                    break;
                }
                else
                {
                    //No exact match, lets can the Inferface next
                    ClassScanner scanner = scan(anInterface);
                    match = scanner.isMatch();
                    className = s;
                }

            }
        }
        //We're processing java.lang.Object
        else if (superName == null)
        {
            return;
        }
        else
        {
            //Lets check the super class
            ClassScanner scanner = scan(superName);
            match = scanner.isMatch();
            className = scanner.getClassName();
            //If there is a match we need to set the super class not the subclass that matched
            if(match)
            {
                className = s;
            }
        }
    }

    protected ClassScanner scan(String name)
    {
        try
        {
            InterfaceClassScanner scanner = new InterfaceClassScanner(interfaceClass, classLoader);
            URL classURL = getClassURL(name);
            if(classURL==null)
            {
                throw new RuntimeException("Failed to read class URL for name: " + name);
            }
            InputStream classStream = classURL.openStream();
            ClassReader r = new ClosableClassReader(classStream);
            
            r.accept(scanner, 0);
            return scanner;
        }
        catch (IOException e)
        {
            throw new RuntimeException(name, e);
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
