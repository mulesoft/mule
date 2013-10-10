/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.launcher;

import org.mule.util.StringUtils;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * TODO document overrides, blocked, systemPackages and syntax for specifying those.
 */
public class FineGrainedControlClassLoader extends GoodCitizenClassLoader
{

    protected String appName;

    // Finished with '.' so that we can use startsWith to verify
    protected String[] systemPackages = {
            "java.",
            "javax.",
            "org.mule.",
            "com.mulesoft.",
            "com.mulesource."
    };

    protected Set<String> overrides = new HashSet<String>();
    protected Set<String> blocked = new HashSet<String>();

    public FineGrainedControlClassLoader(URL[] urls, ClassLoader parent)
    {
        this(urls, parent, Collections.<String>emptySet());
    }

    public FineGrainedControlClassLoader(URL[] urls, ClassLoader parent, Set<String> overrides)
    {
        super(urls, parent);
        processOverrides(overrides);
    }

    protected void processOverrides(Set<String> overrides)
    {
        if (overrides != null && !overrides.isEmpty())
        {
            for (String override : overrides)
            {
                override = StringUtils.defaultString(override).trim();
                // 'blocked' package definitions come with a '-' prefix
                if (override.startsWith("-"))
                {
                    override = override.substring(1);
                    this.blocked.add(override);
                }
                this.overrides.add(override);

                for (String systemPackage : systemPackages)
                {
                    if (override.startsWith(systemPackage))
                    {
                        throw new IllegalArgumentException("Can't override a system package. Offending value: " + override);
                    }
                }
            }
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        Class<?> result = findLoadedClass(name);

        if (result != null)
        {
            return result;
        }
        boolean overrideMatch = isOverridden(name);


        if (overrideMatch)
        {
            boolean blockedMatch = isBlocked(name);

            if (blockedMatch)
            {
                // load this class from the child ONLY, don't attempt parent, let CNFE exception propagate
                result = findClass(name);
            }
            else
            {
                // load this class from the child
                try
                {
                    result = findClass(name);
                }
                catch (ClassNotFoundException e)
                {
                    // let it fail with CNFE
                    result = findParentClass(name);
                }
            }


        }
        else
        {
            // no overrides, regular parent-first lookup
            try
            {
                result = findParentClass(name);
            }
            catch (ClassNotFoundException e)
            {
                result = findClass(name);
            }
        }

        if (resolve)
        {
            resolveClass(result);
        }

        return result;
    }

    protected boolean isOverridden(String name)
    {
        // find a match
        boolean overrideMatch = false;
        for (String override : overrides)
        {
            if (name.equals(override) || name.startsWith(override + "."))
            {
                overrideMatch = true;
                break;
            }
        }
        return overrideMatch;
    }

    protected boolean isBlocked(String name)
    {
        boolean blockedMatch = false;
        for (String b : blocked)
        {
            if (name.equals(b) || name.startsWith(b + "."))
            {
                blockedMatch = true;
                break;
            }
        }
        return blockedMatch;
    }

    protected Class<?> findParentClass(String name) throws ClassNotFoundException
    {
        if (getParent() != null)
        {
            return getParent().loadClass(name);
        }
        else
        {
            return findSystemClass(name);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException
    {
        return super.findClass(name);
    }

}
