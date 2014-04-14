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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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

    public static final char COMMA_SEPARATOR = ',';
    public static final String COLON_SEPARATOR = ":";
    public String TRANSFORMER_KEY = ".transformer.";
    public String OBJECT_KEY = ".object.";
    public String SINGLE_TX = ".singletx.";

    protected final transient Log logger = LogFactory.getLog(getClass());

    protected MuleContext context;

    private final RegistryBootstrapDiscoverer discoverer;

    public SimpleRegistryBootstrap()
    {
        this(new DefaultRegistryBootstrapDiscoverer());
    }

    public SimpleRegistryBootstrap(RegistryBootstrapDiscoverer discoverer)
    {
        this.discoverer = discoverer;
    }

    /**
     * {@inheritDoc}
     */
    public void setMuleContext(MuleContext context)
    {
        this.context = context;
    }

    /**
     * {@inheritDoc}
     */
    public void initialise() throws InitialisationException
    {
        List<Properties> bootstraps;
        try
        {
            bootstraps = discoverer.discover();
        }
        catch (Exception e1)
        {
            throw new InitialisationException(e1, this);
        }

        // ... and only then merge and process them
        int objectCounter = 1;
        int transformerCounter = 1;
        List<BootstrapProperty> transformers = new ArrayList<BootstrapProperty>();
        List<BootstrapProperty> namedObjects = new ArrayList<BootstrapProperty>();
        List<BootstrapProperty> unnamedObjects = new ArrayList<BootstrapProperty>();
        List<BootstrapProperty> singleTransactionFactories = new ArrayList<BootstrapProperty>();

        for (Properties bootstrap : bootstraps)
        {
            for (Map.Entry entry : bootstrap.entrySet())
            {
                final BootstrapProperty bootstrapProperty = parseBootstrapProperty((String) entry.getKey(), (String) entry.getValue());
                if (bootstrapProperty.keyContains(OBJECT_KEY))
                {
                    bootstrapProperty.disambiguateKey(objectCounter++);
                    unnamedObjects.add(bootstrapProperty);
                }
                else if (bootstrapProperty.keyContains(TRANSFORMER_KEY) || bootstrapProperty.isOfType(Transformer.class))
                {
                    if (bootstrapProperty.keyContains(TRANSFORMER_KEY))
                    {
                        bootstrapProperty.disambiguateKey(transformerCounter++);
                    }
                    transformers.add(bootstrapProperty);
                }
                else if (bootstrapProperty.keyContains(SINGLE_TX))
                {
                    if (!bootstrapProperty.keyContains(".transaction.resource"))
                    {
                        String transactionResourceKey = bootstrapProperty.key.replace(".transaction.factory", ".transaction.resource");
                        String transactionResource = bootstrap.getProperty(transactionResourceKey);
                        if (transactionResource == null)
                        {
                            throw new InitialisationException(CoreMessages.createStaticMessage(String.format("Theres no transaction resource specified for transaction factory %s", bootstrapProperty.key)), this);
                        }
                        singleTransactionFactories.add(parseBootstrapProperty(bootstrapProperty.className, transactionResource));
                    }
                }
                else
                {
                    namedObjects.add(bootstrapProperty);
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

    private void registerTransactionFactories(List<BootstrapProperty> singleTransactionFactories, MuleContext context) throws Exception
    {
        for (BootstrapProperty bootstrapProperty : singleTransactionFactories)
        {
            try
            {
                context.getTransactionFactoryManager().registerTransactionFactory(Class.forName(bootstrapProperty.className), (TransactionFactory) Class.forName(bootstrapProperty.key).newInstance());

            }
            catch (NoClassDefFoundError ncdfe)
            {
                throwExceptionIfNotOptional(bootstrapProperty.optional, ncdfe, "Ignoring optional transaction factory: " + bootstrapProperty.className);
            }
            catch (ClassNotFoundException cnfe)
            {
                throwExceptionIfNotOptional(bootstrapProperty.optional, cnfe, "Ignoring optional transaction factory: " + bootstrapProperty.className);
            }

        }
    }

    private void registerTransformers(List<BootstrapProperty> props, MuleRegistry registry) throws Exception
    {
        for (BootstrapProperty bootstrapProperty : props)
        {
            Class<?> returnClass = null;
            try
            {
                if (bootstrapProperty.returnClassName != null)
                {
                    returnClass = bootstrapProperty.returnClassName.equals("byte[]") ? byte[].class : ClassUtils.loadClass(bootstrapProperty.returnClassName, getClass());
                }
                Transformer trans = (Transformer) ClassUtils.instanciateClass(bootstrapProperty.className);
                if (!(trans instanceof DiscoverableTransformer))
                {
                    throw new RegistrationException(CoreMessages.transformerNotImplementDiscoverable(trans));
                }
                if (returnClass != null)
                {
                    trans.setReturnDataType(DataTypeFactory.create(returnClass, bootstrapProperty.mime));
                }
                if (bootstrapProperty.objectName == null)
                {
                    // This will generate a default name for the transformer
                    // We then prefix the name to ensure there is less chance of conflict if the user registers
                    // the transformer with the same name
                    bootstrapProperty.objectName = "_" + trans.getName();
                }
                trans.setName(bootstrapProperty.objectName);
                registry.registerTransformer(trans);
            }
            catch (InvocationTargetException itex)
            {
                Throwable cause = ExceptionUtils.getCause(itex);
                throwExceptionIfNotOptional(cause instanceof NoClassDefFoundError && bootstrapProperty.optional, cause, "Ignoring optional transformer: " + bootstrapProperty.className);
            }
            catch (NoClassDefFoundError ncdfe)
            {
                throwExceptionIfNotOptional(bootstrapProperty.optional, ncdfe, "Ignoring optional transformer: " + bootstrapProperty.className);

            }
            catch (ClassNotFoundException cnfe)
            {
                throwExceptionIfNotOptional(bootstrapProperty.optional, cnfe, "Ignoring optional transformer: " + bootstrapProperty.className);
            }
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

    private void registerObjects(List<BootstrapProperty> props, Registry registry) throws Exception
    {
        for (BootstrapProperty bootstrapProperty : props)
        {
            registerObject(bootstrapProperty.key, bootstrapProperty.className, bootstrapProperty.optional, registry);
        }
        props.clear();
    }

    private void registerUnnamedObjects(List<BootstrapProperty> props, Registry registry) throws Exception
    {
        for (BootstrapProperty bootstrapProperty : props)
        {
            final String key = String.format("%s#%s", bootstrapProperty.key, UUID.getUUID());
            registerObject(key, bootstrapProperty.className, bootstrapProperty.optional, registry);
        }
        props.clear();
    }

    private void registerObject(String key, String className, boolean optional, Registry registry) throws Exception
    {

        try
        {
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
                o = ((BootstrapObjectFactory) o).create();
            }
            registry.registerObject(key, o, meta);
        }
        catch (InvocationTargetException itex)
        {
            Throwable cause = ExceptionUtils.getCause(itex);
            throwExceptionIfNotOptional(cause instanceof NoClassDefFoundError && optional, cause, "Ignoring optional object: " + className);
        }
        catch (NoClassDefFoundError ncdfe)
        {
            throwExceptionIfNotOptional(optional, ncdfe, "Ignoring optional object: " + className);
        }
        catch (ClassNotFoundException cnfe)
        {
            throwExceptionIfNotOptional(optional, cnfe, "Ignoring optional object: " + className);
        }
    }

    private class BootstrapProperty
    {

        String key;
        String className;
        boolean optional;
        public String objectName;
        public String returnClassName;
        private Class<?> classObject;
        public String mime;

        public boolean isOfType(Class<?> parentClass)
        {
            if (classObject == null)
            {
                try
                {
                    classObject = ClassUtils.getClass(className);
                }
                catch (ClassNotFoundException e)
                {
                    return false;
                }
            }
            return parentClass.isAssignableFrom(classObject);
        }

        public boolean keyContains(String text)
        {
            return key != null && key.contains(text);
        }

        public void disambiguateKey(int counter)
        {
            int lastPointPosition = key.lastIndexOf(".");
            if (lastPointPosition > -1)
            {
                key = key.substring(0, lastPointPosition) + counter;
            }
        }
    }

    private BootstrapProperty parseBootstrapProperty(String propertyName, String propertyValue)
    {
        BootstrapProperty bootstrapProperty = new BootstrapProperty();
        bootstrapProperty.key = propertyName;
        bootstrapProperty.className = propertyValue;

        int firstCommaPosition = propertyValue.indexOf(COMMA_SEPARATOR);
        if (firstCommaPosition > -1)
        {
            bootstrapProperty.className = propertyValue.substring(0, firstCommaPosition);

            Properties p = PropertiesUtils.getPropertiesFromString(propertyValue.substring(firstCommaPosition + 1), COMMA_SEPARATOR);
            bootstrapProperty.optional = p.containsKey("optional");
            bootstrapProperty.objectName = p.getProperty("name", null);
            bootstrapProperty.returnClassName = p.getProperty("returnClass", null);

            bootstrapProperty.mime = null;
            if (bootstrapProperty.returnClassName != null)
            {
                int colonPosition = bootstrapProperty.returnClassName.indexOf(COLON_SEPARATOR);
                if (colonPosition > -1)
                {
                    bootstrapProperty.mime = bootstrapProperty.returnClassName.substring(colonPosition + 1);
                    bootstrapProperty.returnClassName = bootstrapProperty.returnClassName.substring(0, colonPosition);
                }
            }

        }
        return bootstrapProperty;
    }

    private void throwExceptionIfNotOptional(boolean optional, Throwable t, String message) throws Exception
    {
        if (optional)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(message);
            }
        }
        else if (t instanceof Exception)
        {
            throw (Exception) t;
        }
        else
        {
            throw new Exception(t);
        }
    }
}

