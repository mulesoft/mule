/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.bootstrap;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.registry.ObjectProcessor;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.Registry;
import org.mule.api.registry.TransformerResolver;
import org.mule.api.transaction.TransactionFactory;
import org.mule.api.transformer.Converter;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.Transformer;
import org.mule.api.util.StreamCloser;
import org.mule.config.i18n.CoreMessages;
import org.mule.registry.MuleRegistryHelper;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.ClassUtils;
import org.mule.util.ExceptionUtils;
import org.mule.util.PropertiesUtils;
import org.mule.util.UUID;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This object will load objects defined in a file called <code>registry-bootstrap.properties</code> into the local registry.
 * This allows modules and transports to make certain objects available by default.  The most common use case is for a
 * module or transport to load stateless transformers into the registry.
 * For this file to be located it must be present in the modules META-INF directory under
 * <pre>META-INF/services/org/mule/config/</pre>
 * <p/>
 * The format of this file is a simple key / value pair. i.e.
 * <pre>
 * myobject=org.foo.MyObject
 * </pre>
 * Will register an instance of MyObject with a key of 'myobject'. If you don't care about the object name and want to
 * ensure that the ojbect gets a unique name you can use -
 * <pre>
 * object.1=org.foo.MyObject
 * object.2=org.bar.MyObject
 * </pre>
 * or
 * <pre>
 * myFoo=org.foo.MyObject
 * myBar=org.bar.MyObject
 * </pre>
 * Loading transformers has a slightly different notation since you can define the 'returnClass' with optional mime type, and 'name'of
 * the transformer as parameters i.e.
 * <pre>
 * transformer.1=org.mule.transport.jms.transformers.JMSMessageToObject,returnClass=byte[]
 * transformer.2=org.mule.transport.jms.transformers.JMSMessageToObject,returnClass=java.lang.String:text/xml, name=JMSMessageToString
 * transformer.3=org.mule.transport.jms.transformers.JMSMessageToObject,returnClass=java.util.Hashtable)
 * </pre>
 * Note that the key used for transformers must be 'transformer.x' where 'x' is a sequential number.  The transformer name will be
 * automatically generated as JMSMessageToXXX where XXX is the return class name i.e. JMSMessageToString unless a 'name'
 * parameter is specified. If no 'returnClass' is specified the default in the transformer will be used.
 * <p/>
 * Note that all objects defined have to have a default constructor. They can implement injection interfaces such as
 * {@link org.mule.api.context.MuleContextAware} and lifecycle interfaces such as {@link org.mule.api.lifecycle.Initialisable}.
 */
public class SimpleRegistryBootstrap implements Initialisable, MuleContextAware
{
    public static final String SERVICE_PATH = "META-INF/services/org/mule/config/";

    public static final String REGISTRY_PROPERTIES = "registry-bootstrap.properties";

    public String TRANSFORMER_KEY = ".transformer.";
    public String OBJECT_KEY = ".object.";
    public String SINGLE_TX = ".singletx.";

    protected final transient Log logger = LogFactory.getLog(getClass());

    protected MuleContext context;

    /** {@inheritDoc} */
    public void setMuleContext(MuleContext context)
    {
        this.context = context;
    }
    
    private Map<String, String> readBootstrap(URL url) throws Exception
    {
        InputStreamReader in = null;
        Map<String, String> bootstrap = new LinkedHashMap<String, String>();
        try
        {
            in = new InputStreamReader(url.openStream());
            BufferedReader reader = new BufferedReader(in);
            String line = null;

            while ((line = reader.readLine()) != null)
            {
                if (StringUtils.isBlank(line) || line.startsWith("#"))
                {
                    continue;
                }
                String[] pair = line.split("=");
                if (pair.length < 2)
                {
                    continue;
                }
                else if (pair.length > 2)
                {
                    for (int i = 2; i < pair.length; i++)
                    {
                        pair[1] += "=" + pair[i];
                    }
                }
                bootstrap.put(pair[0], pair[1]);
            }

            return bootstrap;

        }
        finally
        {
            if (in != null)
            {
                in.close();
            }
        }
    }

    /** {@inheritDoc} */
    public void initialise() throws InitialisationException
    {
        Enumeration<?> e = ClassUtils.getResources(SERVICE_PATH + REGISTRY_PROPERTIES, getClass());
        List<Map<String, String>> bootstraps = new LinkedList<Map<String, String>>();

        // load ALL of the bootstrap files first
        while (e.hasMoreElements())
        {
            try
            {
                bootstraps.add(this.readBootstrap((URL) e.nextElement()));
            }
            catch (Exception e1)
            {
                throw new InitialisationException(e1, this);
            }
        }

        // ... and only then merge and process them
        int objectCounter = 1;
        int transformerCounter = 1;
        
        Map<String, String> transformers = new LinkedHashMap<String, String>();
        Map<String, String> namedObjects = new LinkedHashMap<String, String>();
        Map<String, String> unnamedObjects = new LinkedHashMap<String, String>();
        Map<String, String> singleTransactionFactories = new LinkedHashMap<String, String>();

        for (Map<String, String> bootstrap : bootstraps)
        {
            for (Map.Entry<String, String> entry : bootstrap.entrySet())
            {
                final String key = entry.getKey();
                if (key.contains(OBJECT_KEY))
                {
                    String newKey = key.substring(0, key.lastIndexOf(".")) + objectCounter++;
                    unnamedObjects.put(newKey, entry.getValue());
                }
                else if (key.contains(TRANSFORMER_KEY))
                {
                    String newKey = key.substring(0, key.lastIndexOf(".")) + transformerCounter++;
                    transformers.put(newKey, entry.getValue());
                }
                else if (key.contains(SINGLE_TX))
                {
                    if (!key.contains(".transaction.resource"))
                    {
                        String transactionResourceKey = key.replace(".transaction.factory",".transaction.resource");
                        String transactionResource = bootstrap.get(transactionResourceKey);
                        if (transactionResource == null)
                        {
                            throw new InitialisationException(CoreMessages.createStaticMessage(String.format("Theres no transaction resource specified for transaction factory %s",key)),this);
                        }
                        singleTransactionFactories.put(entry.getValue(),transactionResource);
                    }
                }
                else
                {
                    // we allow arbitrary keys in the registry-bootstrap.properties but since we're
                    // aggregating multiple files here we must make sure that the keys are unique
//                    if (accumulatedProps.getProperty(key) != null)
//                    {
//                        throw new IllegalStateException(
//                                "more than one registry-bootstrap.properties file contains a key " + key);
//                    }
//                    else
                    {
                        namedObjects.put(key, entry.getValue());
                    }
                }
            }
        }

        try
        {
            registerUnnamedObjects(unnamedObjects, context.getRegistry());
            registerTransformers((MuleRegistryHelper) context.getRegistry());
            registerTransformers(transformers, context.getRegistry());
            registerObjects(namedObjects, context.getRegistry());
            registerTransactionFactories(singleTransactionFactories, context);
        }
        catch (Exception e1)
        {
            throw new InitialisationException(e1, this);
        }
    }

    private void registerTransactionFactories(Map<String, String> singleTransactionFactories, MuleContext context) throws Exception
    {
        for (String transactionFactoryClassName : singleTransactionFactories.keySet())
        {
            String transactionResourceClassName = singleTransactionFactories.get(transactionFactoryClassName);
            context.getTransactionFactoryManager().registerTransactionFactory(Class.forName(transactionResourceClassName), (TransactionFactory) Class.forName(transactionFactoryClassName).newInstance());
        }
    }

    private void registerTransformers(Map<String, String> bootstrap, MuleRegistry registry) throws Exception
    {
        String transString;
        String name = null;
        String returnClassString;
        boolean optional = false;

        for (Map.Entry<String, String> entry : bootstrap.entrySet())
        {
            transString = entry.getValue();
            // reset
            Class<?> returnClass = null;
            returnClassString = null;
            int x = transString.indexOf(",");
            if (x > -1)
            {
                Properties p = PropertiesUtils.getPropertiesFromString(transString.substring(x + 1), ',');
                name = p.getProperty("name", null);
                returnClassString = p.getProperty("returnClass", null);
                optional = p.containsKey("optional");
            }

            final String transClass = (x == -1 ? transString : transString.substring(0, x));
            try
            {
                String mime = null;
                if (returnClassString != null)
                {
                    int i = returnClassString.indexOf(":");
                    if(i > -1)
                    {
                        mime = returnClassString.substring(i + 1);
                        returnClassString = returnClassString.substring(0, i);
                    }
                    if (returnClassString.equals("byte[]"))
                    {
                        returnClass = byte[].class;
                    }
                    else
                    {
                        returnClass = ClassUtils.loadClass(returnClassString, getClass());
                    }
                }
                Transformer trans = (Transformer) ClassUtils.instanciateClass(transClass);
                if (!(trans instanceof DiscoverableTransformer))
                {
                    throw new RegistrationException(CoreMessages.transformerNotImplementDiscoverable(trans));
                }
                if (returnClass != null)
                {
                    trans.setReturnDataType(DataTypeFactory.create(returnClass, mime));
                }
                if (name != null)
                {
                    trans.setName(name);
                }
                else
                {
                    //This will generate a default name for the transformer
                    name = trans.getName();
                    //We then prefix the name to ensure there is less chance of conflict if the user registers
                    // the transformer with the same name
                    trans.setName("_" + name);
                }
                registry.registerTransformer(trans);
            }
            catch (InvocationTargetException itex)
            {
                Throwable cause = ExceptionUtils.getCause(itex);
                if (cause instanceof NoClassDefFoundError && optional)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Ignoring optional transformer: " + transClass);
                    }
                }
                else
                {
                    throw new Exception(cause);
                }
            }
            catch (NoClassDefFoundError ncdfe)
            {
                if (optional)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Ignoring optional transformer: " + transClass);
                    }
                }
                else
                {
                    throw ncdfe;
                }
            }
            catch (ClassNotFoundException cnfe)
            {
                if (optional)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Ignoring optional transformer: " + transClass);
                    }
                }
                else
                {
                    throw cnfe;
                }
            }

            name = null;
            returnClass = null;
        }
    }

    private void registerTransformers(MuleRegistryHelper registry) throws MuleException
    {
        Map<String, Converter> converters = registry.lookupByType(Converter.class);
        for (Converter converter : converters.values())
        {
            registry.notifyTransformerResolvers(converter, TransformerResolver.RegistryAction.ADDED);
        }
    }

    private void registerObjects(Map<String, String> props, Registry registry) throws Exception
    {
        for (Map.Entry<String, String> entry : props.entrySet())
        {
            registerObject(entry.getKey(), entry.getValue(), registry);
        }
        props.clear();
    }

    private void registerUnnamedObjects(Map<String, String> bootstrap, Registry registry) throws Exception
    {
        for (Map.Entry<String, String> entry : bootstrap.entrySet())
        {
            final String key = String.format("%s#%s", entry.getKey(), UUID.getUUID());
            registerObject(key, entry.getValue(), registry);
        }
        bootstrap.clear();
    }

    private void registerObject(String key, String value, Registry registry) throws Exception
    {
        boolean optional = false;
        String className = null;

        try
        {
            int x = value.indexOf(",");
            if (x > -1)
            {
                Properties p = PropertiesUtils.getPropertiesFromString(value.substring(x + 1), ',');
                optional = p.containsKey("optional");
                className = value.substring(0, x);
            }
            else
            {
                className = value;
            }
            Object o = ClassUtils.instanciateClass(className);
            Class<?> meta = Object.class;

            if (o instanceof ObjectProcessor)
            {
                meta = ObjectProcessor.class;
            }
            else if (o instanceof StreamCloser)
            {
                meta = StreamCloser.class;
            }
            else if (o instanceof BootstrapObjectFactory)
            {
                o = ((BootstrapObjectFactory)o).create();
            }
            registry.registerObject(key, o, meta);
        }
        catch (InvocationTargetException itex)
        {
            Throwable cause = ExceptionUtils.getCause(itex);
            if (cause instanceof NoClassDefFoundError && optional)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Ignoring optional object: " + className);
                }
            }
            else
            {
                throw new Exception(cause);
            }
        }
        catch (NoClassDefFoundError ncdfe)
        {
            if (optional)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Ignoring optional object: " + className);
                }
            }
            else
            {
                throw ncdfe;
            }
        }
        catch (ClassNotFoundException cnfe)
        {
            if (optional)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Ignoring optional object: " + className);
                }
            }
            else
            {
                throw cnfe;
            }
        }
    }
}
