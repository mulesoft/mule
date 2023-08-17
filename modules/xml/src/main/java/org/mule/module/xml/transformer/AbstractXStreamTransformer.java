/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.transformer;

import static org.mule.api.config.MuleProperties.MULE_ENABLE_XSTREAM_DENYLIST;
import static org.mule.api.config.MuleProperties.MULE_XSTREAM_ALLOWLIST;

import static java.lang.Boolean.getBoolean;
import static java.lang.System.getProperty;

import org.mule.api.config.MuleProperties;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.MessageFactory;
import org.mule.transformer.AbstractMessageTransformer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.security.AnyTypePermission;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <code>AbstractXStreamTransformer</code> is a base class for all XStream based
 * transformers. It takes care of creating and configuring the XStream parser.
 */

public abstract class AbstractXStreamTransformer extends AbstractMessageTransformer
{
    private final AtomicReference<XStream> xstream = new AtomicReference<XStream>();
    private volatile String driverClass = XStreamFactory.XSTREAM_XPP_DRIVER;
    private volatile Map<String, Class<?>> aliases = new HashMap<String, Class<?>>();
    private volatile Set<Class <? extends Converter>> converters = new HashSet<Class <? extends Converter>>();
    protected boolean isDenylistEnabled = getBoolean(MULE_ENABLE_XSTREAM_DENYLIST);
    private String allowlist = getProperty(MULE_XSTREAM_ALLOWLIST);

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        try
        {
            // Create XStream instance as part of initialization so that we can set
            // the context classloader that will be required to load classes.
            XStream xStreamInstance = getXStream();
            xStreamInstance.setClassLoader(Thread.currentThread().getContextClassLoader());
        }
        catch (TransformerException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    public final XStream getXStream() throws TransformerException
    {
        XStream instance = xstream.get();

        if (instance == null)
        {
            try
            {
                instance = new XStreamFactory(driverClass, aliases, converters).getInstance();
                if (!xstream.compareAndSet(null, instance))
                {
                    instance = xstream.get();
                }
                // Configure security
                if (isDenylistEnabled) {
                    instance.addPermission(AnyTypePermission.ANY);
                    instance.denyTypes(new String[]{ "javax.imageio.ImageIO$ContainsFilter", "sun.awt.datatransfer.DataTransferer$IndexOrderComparator", "com.sun.tools.javac.processing.JavacProcessingEnvironment$NameProcessIterator" });
                    instance.denyTypes(new Class[]{ java.lang.ProcessBuilder.class, java.beans.EventHandler.class, java.lang.ProcessBuilder.class, java.lang.Void.class, void.class });
                    instance.denyTypesByRegExp(new String[]{ ".*\\$ServiceNameIterator", "javafx\\.collections\\.ObservableList\\$.*", ".*\\.bcel\\..*\\.util\\.ClassLoader", ".*\\$GetterSetterReflection", ".*\\$LazyIterator", ".*\\$PrivilegedGetter",  ".*\\.ws\\.client\\.sei\\..*", ".*\\$ProxyLazyValue", "com\\.sun\\.jndi\\..*Enumerat(?:ion|tor)", ".*\\$URLData", ".*\\.xsltc\\.trax\\.TemplatesImpl" });
                    instance.denyTypesByWildcard(new String[]{ "sun.reflect.**", "sun.tracing.**", "com.sun.corba.**" });
                    instance.denyTypeHierarchy(java.io.InputStream.class);
                    instance.denyTypeHierarchy(java.nio.channels.Channel.class);
                    instance.denyTypeHierarchy(javax.activation.DataSource.class);
                    instance.denyTypeHierarchy(javax.sql.rowset.BaseRowSet.class);
                } else if (allowlist != null) {
                    // Parse the allowlist and call allowTypes and allowTypesByWildcard accordingly
                    String[] allowlistItems = allowlist.split(",");
                    for (String item : allowlistItems) {
                        if (item.endsWith("*")) {
                            instance.allowTypesByWildcard(new String[]{item});
                        } else {
                            instance.allowTypes(new String[]{item});
                        }
                    }
                }
            }
            catch (Exception e)
            {
                throw new TransformerException(MessageFactory.createStaticMessage("Unable to initialize XStream"), e);
            }
        }

        return instance;
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        AbstractXStreamTransformer clone = (AbstractXStreamTransformer) super.clone();
        clone.setDriverClass(driverClass);

        if (aliases != null)
        {
            clone.setAliases(new HashMap<String, Class<?>>(aliases));
        }

        if (converters != null)
        {
            clone.setConverters(new HashSet<Class <? extends Converter>>(converters));
        }

        return clone;
    }

    public String getDriverClass()
    {
        return driverClass;
    }

    public void setDriverClass(String driverClass)
    {
        this.driverClass = driverClass;
        // force XStream instance update
        this.xstream.set(null);
    }

    public Map<String, Class<?>> getAliases()
    {
        return aliases;
    }

    public void setAliases(Map<String, Class<?>> aliases)
    {
        this.aliases = aliases;
        // force XStream instance update
        this.xstream.set(null);
    }

    public Set<Class <? extends Converter>> getConverters()
    {
        return converters;
    }

    public void setConverters(Set<Class <? extends Converter>> converters)
    {
        this.converters = converters;
        // force XStream instance update
        this.xstream.set(null);
    }

    public void addAlias(String alias, Class<?> aClass)
    {
        aliases.put(alias, aClass);
    }

    public Class<?> removeAlias(String alias)
    {
        return aliases.remove(alias);
    }

    public void addConverter(Class<? extends Converter> converterClass)
    {
        converters.add(converterClass);
    }

    public boolean removeAlias(Class<? extends Converter> converterClass)
    {
        return converters.remove(converterClass);
    }
}
