/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.TransformerResolver;
import org.mule.api.transaction.TransactionFactory;
import org.mule.api.transformer.Converter;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.Transformer;
import org.mule.config.bootstrap.BootstrapException;
import org.mule.config.bootstrap.BootstrapObjectFactory;
import org.mule.config.bootstrap.ClassPathRegistryBootstrapDiscoverer;
import org.mule.config.bootstrap.RegistryBootstrapDiscoverer;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.spring.factories.BootstrapObjectFactoryBean;
import org.mule.registry.MuleRegistryHelper;
import org.mule.transformer.TransformerUtils;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.ClassUtils;
import org.mule.util.ExceptionUtils;
import org.mule.util.OrderedProperties;
import org.mule.util.PropertiesUtils;
import org.mule.util.UUID;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;

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
 * It's also possible to define if the entry must be applied to a domain, an application, or both by using the parameter applyToArtifactType.
 * <pre>
 * myFoo=org.foo.MyObject will be applied to any mule application since the parameter applyToArtifactType default value is app
 * myFoo=org.foo.MyObject;applyToArtifactType=app will be applied to any mule application
 * myFoo=org.foo.MyObject;applyToArtifactType=domain will be applied to any mule domain
 * myFoo=org.foo.MyObject;applyToArtifactType=app/domain will be applied to any mule application and any mule domain
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
 * {@link MuleContextAware} and lifecycle interfaces such as {@link Initialisable}.
 */
public class SpringRegistryBootstrap implements MuleContextAware
{

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    public String TRANSFORMER_KEY = ".transformer.";
    public String OBJECT_KEY = ".object.";
    public String SINGLE_TX = ".singletx.";

    private final RegistryBootstrapDiscoverer discoverer;
    private final BeanDefinitionRegistry beanDefinitionRegistry;

    private ArtifactType supportedArtifactType = ArtifactType.APP;
    protected MuleContext context;

    public enum ArtifactType
    {
        APP("app"), DOMAIN("domain"), ALL("app/domain");

        public static final String APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY = "applyToArtifactType";
        private final String artifactTypeAsString;

        ArtifactType(String artifactTypeAsString)
        {
            this.artifactTypeAsString = artifactTypeAsString;
        }

        public String getAsString()
        {
            return this.artifactTypeAsString;
        }

        public static ArtifactType createFromString(String artifactTypeAsString)
        {
            for (ArtifactType artifactType : values())
            {
                if (artifactType.artifactTypeAsString.equals(artifactTypeAsString))
                {
                    return artifactType;
                }
            }
            throw new MuleRuntimeException(createStaticMessage("No artifact type found for value: " + artifactTypeAsString));
        }
    }

    /**
     * Creates a default SimpleRegistryBootstrap using a {@link ClassPathRegistryBootstrapDiscoverer}
     * in order to get the Properties resources from the class path.
     */
    public SpringRegistryBootstrap(BeanDefinitionRegistry beanDefinitionRegistry)
    {
        this(new ClassPathRegistryBootstrapDiscoverer(), beanDefinitionRegistry);
    }

    /**
     * Allows to specify a {@link RegistryBootstrapDiscoverer} to discover the Properties
     * resources to be used.
     * @param discoverer
     */
    public SpringRegistryBootstrap(RegistryBootstrapDiscoverer discoverer, BeanDefinitionRegistry beanDefinitionRegistry)
    {
        this.discoverer = discoverer;
        this.beanDefinitionRegistry = beanDefinitionRegistry;
    }

    /**
     * {@inheritDoc}
     */
    public void setMuleContext(MuleContext context)
    {
        this.context = context;
    }

    public void boot() throws BootstrapException
    {
        List<Properties> bootstraps = discoverer.discover();

        // Merge and process properties
        int objectCounter = 1;
        int transformerCounter = 1;
        Properties transformers = new OrderedProperties();
        Properties namedObjects = new OrderedProperties();
        Properties unnamedObjects = new OrderedProperties();
        Map<String,String> singleTransactionFactories = new LinkedHashMap<String,String>();

        for (Properties bootstrap : bootstraps)
        {
            for (Entry entry : bootstrap.entrySet())
            {
                final String key = (String) entry.getKey();
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
                        String transactionResource = bootstrap.getProperty(transactionResourceKey);
                        if (transactionResource == null)
                        {
                            throw new BootstrapException(createStaticMessage(String.format("There's no transaction resource specified for transaction factory %s", key)));
                        }
                        singleTransactionFactories.put((String) entry.getValue(),transactionResource);
                    }
                }
                else
                {
                    namedObjects.put(key, entry.getValue());
                }
            }
        }

        try
        {
            registerUnnamedObjects(unnamedObjects);
            registerTransformers((MuleRegistryHelper) context.getRegistry());
            registerTransformers(transformers);
            registerObjects(namedObjects);
            registerTransactionFactories(singleTransactionFactories, context);
        }
        catch (Exception e1)
        {
            throw new BootstrapException(createStaticMessage("Exception found registering bootstrap objects"), e1);
        }
    }

    private void registerTransactionFactories(Map<String, String> singleTransactionFactories, MuleContext context) throws Exception
    {
        for (Entry<String, String> entry : singleTransactionFactories.entrySet())
        {
            String transactionResourceClassNameProperties = entry.getValue();
            String transactionFactoryClassName = entry.getKey();
            boolean optional = false;
            // reset
            int x = transactionResourceClassNameProperties.indexOf(",");
            if (x > -1)
            {
                Properties p = PropertiesUtils.getPropertiesFromString(transactionResourceClassNameProperties.substring(x + 1), ',');
                optional = p.containsKey("optional");
            }
            final String transactionResourceClassName = (x == -1 ? transactionResourceClassNameProperties : transactionResourceClassNameProperties.substring(0, x));
            try
            {
                context.getTransactionFactoryManager().registerTransactionFactory(Class.forName(transactionResourceClassName), (TransactionFactory) Class.forName(transactionFactoryClassName).newInstance());

            }
            catch (NoClassDefFoundError ncdfe)
            {
                throwExceptionIfNotOptional(optional,ncdfe,"Ignoring optional transaction factory: " + transactionResourceClassName);
            }
            catch (ClassNotFoundException cnfe)
            {
                throwExceptionIfNotOptional(optional,cnfe,"Ignoring optional transaction factory: " + transactionResourceClassName);
            }
        }
    }

    private void registerTransformers(Properties props) throws Exception
    {
        String transString;
        String name = null;
        String returnClassString;
        boolean optional = false;

        for (Entry<Object, Object> entry : props.entrySet())
        {
            transString = (String)entry.getValue();
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

            final Class<? extends Transformer> transformerClass = getClass(x == -1 ? transString : transString.substring(0, x));

            if (!DiscoverableTransformer.class.isAssignableFrom(transformerClass))
            {
                throw new RegistrationException(CoreMessages.transformerNotImplementDiscoverable(transformerClass));
            }

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
                        returnClass = getClass(returnClassString);
                    }
                }

                GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
                beanDefinition.setBeanClass(transformerClass);

                MutablePropertyValues properties = new MutablePropertyValues();

                DataType returnType = null;

                if (returnClass != null)
                {
                    returnType = DataTypeFactory.create(returnClass, mime);
                    properties.addPropertyValue("returnDataType", returnType);
                }

                if (name == null)
                {
                    //This will generate a default name for the transformer
                    //We then prefix the name to ensure there is less chance of conflict if the user registers
                    // the transformer with the same name
                    name = "_" + TransformerUtils.generateTransformerName(transformerClass, returnType);
                }

                properties.addPropertyValue("name", name);
                beanDefinition.setPropertyValues(properties);

                beanDefinitionRegistry.registerBeanDefinition(name, beanDefinition);

            }
            catch (NoClassDefFoundError ncdfe)
            {
                throwExceptionIfNotOptional( optional, ncdfe, "Ignoring optional transformer: " + transformerClass);

            }
            catch (ClassNotFoundException cnfe)
            {
                throwExceptionIfNotOptional( optional, cnfe, "Ignoring optional transformer: " + transformerClass);
            }

            name = null;
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

    private void registerObjects(Properties props) throws Exception
    {
        for (Entry<Object, Object> entry : props.entrySet())
        {
            registerObject((String)entry.getKey(), (String)entry.getValue());
        }
        props.clear();
    }

    private void registerUnnamedObjects(Properties props) throws Exception
    {
        for (Entry<Object, Object> entry : props.entrySet())
        {
            final String key = String.format("%s#%s", entry.getKey(), UUID.getUUID());
            registerObject(key, (String) entry.getValue());
        }
        props.clear();
    }

    private void registerObject(String key, String value) throws Exception
    {
        ArtifactType artifactTypeParameterValue = ArtifactType.APP;

        boolean optional = false;
        String className = null;

        try
        {
            int x = value.indexOf(",");
            if (x > -1)
            {
                Properties p = PropertiesUtils.getPropertiesFromString(value.substring(x + 1), ',');
                if (p.containsKey(ArtifactType.APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY))
                {
                    artifactTypeParameterValue = ArtifactType.createFromString((String) p.get(ArtifactType.APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY));
                }
                optional = p.containsKey("optional");
                className = value.substring(0, x);
            }
            else
            {
                className = value;
            }

            if (!artifactTypeParameterValue.equals(ArtifactType.ALL) && !artifactTypeParameterValue.equals(supportedArtifactType))
            {
                return;
            }

            Class<?> clazz = getClass(className);
            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();

            if (BootstrapObjectFactory.class.isAssignableFrom(clazz))
            {
                beanDefinition.setBeanClass(BootstrapObjectFactoryBean.class);
                ConstructorArgumentValues arguments = new ConstructorArgumentValues();
                arguments.addGenericArgumentValue(ClassUtils.instanciateClass(className));
                beanDefinition.setConstructorArgumentValues(arguments);
            }
            else
            {
                beanDefinition.setBeanClass(clazz);
            }

            beanDefinitionRegistry.registerBeanDefinition(key, beanDefinition);
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

    private Class getClass(String className) throws ClassNotFoundException
    {
        return ClassUtils.loadClass(className, getClass());
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

    /**
     * This attributes define which types or registry bootstrap entries will be
     * created depending on the entry applyToArtifactType parameter value.
     *
     * @param supportedArtifactType type of the artifact to support.
     */
    public void setSupportedArtifactType(ArtifactType supportedArtifactType)
    {
        this.supportedArtifactType = supportedArtifactType;
    }
}
