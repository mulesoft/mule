/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.generics;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Interface to discover parameter names for methods and constructors.
 * <p/>
 * <p>Parameter name discovery is not always possible, but various strategies are
 * available to try, such as looking for debug information that may have been
 * emitted at compile time, and looking for argname annotation values optionally
 * accompanying AspectJ annotated methods.
 * <p/>
 * author: Spring
 */
public interface ParameterNameDiscoverer
{

    /**
     * Return parameter names for this method,
     * or <code>null</code> if they cannot be determined.
     *
     * @param method method to find parameter names for
     * @return an array of parameter names if the names can be resolved,
     *         or <code>null</code> if they cannot
     */
    String[] getParameterNames(Method method);

    /**
     * Return parameter names for this constructor,
     * or <code>null</code> if they cannot be determined.
     *
     * @param ctor constructor to find parameter names for
     * @return an array of parameter names if the names can be resolved,
     *         or <code>null</code> if they cannot
     */
    String[] getParameterNames(Constructor ctor);

}
