/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.management.support;

import org.mule.util.ClassUtils;

import java.io.ObjectStreamException;
import java.lang.reflect.Method;

import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Will discover if newer JMX version is available, otherwise fallback to JMX 1.1
 * style support.
 */
// @Immutable
public class AutoDiscoveryJmxSupportFactory implements JmxSupportFactory
{
    /**
     * Safe initialization for a singleton.
     */
    private static final JmxSupportFactory instance = new AutoDiscoveryJmxSupportFactory();

    /**
     * logger used by this class
     */
    private transient Log logger = LogFactory.getLog(getClass());

    /**
     * A cached JMX support class instance.
     */
    private JmxSupport jmxSupport;


    /** Constructs a new AutoDiscoveryJmxSupportFactory. */
    protected AutoDiscoveryJmxSupportFactory ()
    {
        final boolean jmxModernAvailable = isModernSpecAvailable();

        // tertiary operand does not work anymore after hiererachy refactoring ?!
        if (jmxModernAvailable)
        {
            jmxSupport = new JmxModernSupport();
        }
        else
        {
            jmxSupport = new JmxLegacySupport();
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("JMX support instance is " + jmxSupport);
        }
    }

    /**
     * Obtain an instance of the factory class.
     * @return a cached singleton instance
     */
    public static JmxSupportFactory getInstance()
    {
        return instance;
    }


    /**
     * Will try to detect if JMX 1.2 or later is available, otherwise will fallback
     * to the JMX 1.1 version of the support class.
     *
     * @return matching support class instance
     * @see JmxLegacySupport
     */
    public JmxSupport getJmxSupport ()
    {
        return this.jmxSupport;
    }

    /**
     * Is JMX 1.2 and up available for use?
     * @return false if only JMX 1.1 can be used
     */
    protected boolean isModernSpecAvailable ()
    {
        Class<ObjectName> clazz = ObjectName.class;
        // method escape() is available since JMX 1.2
        Method method = ClassUtils.getMethod(clazz, "quote", new Class[]{String.class});

        return  method != null;
    }

    /**
     * Safe deserialization.
     * @throws ObjectStreamException will never throw it for this class
     * @return singleton instance for this classloader/JVM
     */
    private Object readResolve() throws ObjectStreamException
    {
        return instance;
    }
}
