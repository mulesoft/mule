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
import org.mule.util.OrderedProperties;
import org.mule.util.PropertiesUtils;
import org.mule.util.UUID;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
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

    public String TRANSFORMER_KEY = ".transformer.";
    public String OBJECT_KEY = ".object.";
    public String SINGLE_TX = ".singletx.";

    protected final transient Log logger = LogFactory.getLog(getClass());

    protected MuleContext context;

    private final RegistryBoostrapDiscoverer discoverer;

    public SimpleRegistryBootstrap()
    {
        this(new DefaultRegisrtyBoostrapDiscoverer());
    }

    public SimpleRegistryBootstrap(RegistryBoostrapDiscoverer discoverer)
    {
        this.discoverer = discoverer;
    }

    /** {@inheritDoc} */
    public void setMuleContext(MuleContext context)
    {
        this.context = context;
    }

    /** {@inheritDoc} */
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
        List<ParsedProperty> transformers = new ArrayList<ParsedProperty>();
        List<ParsedProperty> namedObjects = new ArrayList<ParsedProperty>();
        List<ParsedProperty> unnamedObjects = new ArrayList<ParsedProperty>();
        List<ParsedProperty> singleTransactionFactories = new ArrayList<ParsedProperty>();

        for (Properties bootstrap : bootstraps)
        {
            for (Map.Entry entry : bootstrap.entrySet())
            {
                final ParsedProperty pp = parsePropertyEntry((String) entry.getKey(), (String) entry.getValue());
                if (pp.keyContains(OBJECT_KEY))
                {
                    pp.disambiguateKey(objectCounter++);
                    unnamedObjects.add(pp);
                }
                else if (pp.keyContains(TRANSFORMER_KEY) || pp.isOfType(Transformer.class))
                {
                    if(pp.keyContains(TRANSFORMER_KEY) ){
                        pp.disambiguateKey(transformerCounter++);
                    }
                    transformers.add(pp);
                }
                else if (pp.keyContains(SINGLE_TX))
                {
                    if (!pp.keyContains(".transaction.resource"))
                    {
                        String transactionResourceKey = pp.key.replace(".transaction.factory",".transaction.resource");
                        String transactionResource = bootstrap.getProperty(transactionResourceKey);
                        if (transactionResource == null)
                        {
                            throw new InitialisationException(CoreMessages.createStaticMessage(String.format("Theres no transaction resource specified for transaction factory %s", pp.key)),this);
                        }
                        singleTransactionFactories.add(parsePropertyEntry(pp.className, transactionResource));
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
                        namedObjects.add(pp);
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

    private void registerTransactionFactories(List<ParsedProperty> singleTransactionFactories, MuleContext context) throws Exception
    {
        for (ParsedProperty pp : singleTransactionFactories)
        {
            try
            {
                context.getTransactionFactoryManager().registerTransactionFactory(Class.forName(pp.className), (TransactionFactory) Class.forName(pp.key).newInstance());

            }
            catch (NoClassDefFoundError ncdfe)
            {
                throwExceptionIfNotOptional(pp.optional,ncdfe,"Ignoring optional transaction factory: " + pp.className);
            }
            catch (ClassNotFoundException cnfe)
            {
                throwExceptionIfNotOptional(pp.optional,cnfe,"Ignoring optional transaction factory: " + pp.className);
            }
            
        }
    }

    private void registerTransformers(List<ParsedProperty> props, MuleRegistry registry) throws Exception
    {
        for (ParsedProperty pp : props)
        {
            Class<?> returnClass = null;
            try
            {
                if (pp.returnClassName != null)
                {
                    returnClass = pp.returnClassName.equals("byte[]") ? byte[].class : ClassUtils.loadClass(pp.returnClassName, getClass());
                }
                Transformer trans = (Transformer) ClassUtils.instanciateClass(pp.className);
                if (!(trans instanceof DiscoverableTransformer))
                {
                    throw new RegistrationException(CoreMessages.transformerNotImplementDiscoverable(trans));
                }
                if (returnClass != null)
                {
                    trans.setReturnDataType(DataTypeFactory.create(returnClass, pp.mime));
                }
                if (pp.objectName == null)
                {
                    // This will generate a default name for the transformer
                    // We then prefix the name to ensure there is less chance of conflict if the user registers
                    // the transformer with the same name
                    pp.objectName = "_" + trans.getName();
                }
                trans.setName(pp.objectName);
                registry.registerTransformer(trans);
            }
            catch (InvocationTargetException itex)
            {
                Throwable cause = ExceptionUtils.getCause(itex);
                throwExceptionIfNotOptional(cause instanceof NoClassDefFoundError && pp.optional, cause, "Ignoring optional transformer: " + pp.className);
            }
            catch (NoClassDefFoundError ncdfe)
            {
                throwExceptionIfNotOptional( pp.optional, ncdfe, "Ignoring optional transformer: " + pp.className);

            }
            catch (ClassNotFoundException cnfe)
            {
                throwExceptionIfNotOptional( pp.optional, cnfe, "Ignoring optional transformer: " + pp.className);
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

    private void registerObjects(List<ParsedProperty> props, Registry registry) throws Exception
    {
        for (ParsedProperty pp : props)
        {
            registerObject(pp.key, pp.className, pp.optional, registry);
        }
        props.clear();
    }

    private void registerUnnamedObjects(List<ParsedProperty> props, Registry registry) throws Exception
    {
        for (ParsedProperty pp : props)
        {
            final String key = String.format("%s#%s", pp.key, UUID.getUUID());
            registerObject(key, pp.className, pp.optional, registry);
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
                o = ((BootstrapObjectFactory)o).create();
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

    private class ParsedProperty
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
            if(classObject==null)
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
            return key!=null && key.contains(text);
        }

        public void disambiguateKey(int counter)
        {
            int lastPointPosition = key.lastIndexOf(".");
            if(lastPointPosition > -1){
                key = key.substring(0, lastPointPosition) + counter;
            }
        }
    }

    private ParsedProperty parsePropertyEntry(String propertyName, String propertyValue)
    {
        ParsedProperty pp = new ParsedProperty();
        pp.key = propertyName;
        pp.className = propertyValue;

        int x = propertyValue.indexOf(",");
        if (x > -1)
        {
            pp.className = propertyValue.substring(0, x);

            Properties p = PropertiesUtils.getPropertiesFromString(propertyValue.substring(x + 1), ',');
            pp.optional = p.containsKey("optional");
            pp.objectName = p.getProperty("name", null);
            pp.returnClassName = p.getProperty("returnClass", null);

            pp.mime = null;
            if (pp.returnClassName != null)
            {
                int i = pp.returnClassName.indexOf(":");
                if(i > -1)
                {
                    pp.mime = pp.returnClassName.substring(i + 1);
                    pp.returnClassName = pp.returnClassName.substring(0, i);
                }
            }

        }
        return pp;
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
        else if ( t instanceof Exception)
        {
            throw (Exception)t;
        }
        else
        {
            throw new Exception(t);
        }
    }
}

interface RegistryBoostrapDiscoverer
{

    List<Properties> discover() throws IOException;
}

class DefaultRegisrtyBoostrapDiscoverer implements RegistryBoostrapDiscoverer
{

    public static final String SERVICE_PATH = "META-INF/services/org/mule/config/";

    public static final String REGISTRY_PROPERTIES = "registry-bootstrap.properties";

    protected final transient Log logger = LogFactory.getLog(getClass());

    @Override
    public List<Properties> discover() throws IOException
    {
        Enumeration<?> e = ClassUtils.getResources(SERVICE_PATH + REGISTRY_PROPERTIES, getClass());
        List<Properties> bootstraps = new LinkedList<Properties>();

        // load ALL of the bootstrap files first
        while (e.hasMoreElements())
        {
            URL url = (URL) e.nextElement();
            if (logger.isDebugEnabled())
            {
                logger.debug("Reading bootstrap file: " + url.toString());
            }
            Properties p = new OrderedProperties();
            p.load(url.openStream());
            bootstraps.add(p);
        }
        return bootstraps;
    }
}