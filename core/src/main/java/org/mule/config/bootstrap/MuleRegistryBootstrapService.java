/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.bootstrap;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
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
import org.mule.config.i18n.MessageFactory;
import org.mule.registry.MuleRegistryHelper;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.ExceptionUtils;
import org.mule.util.PropertiesUtils;
import org.mule.util.UUID;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MuleRegistryBootstrapService implements RegistryBootstrapService
{

    protected final transient Log logger = LogFactory.getLog(getClass());

    public String TRANSFORMER_KEY = ".transformer.";

    public String OBJECT_KEY = ".object.";
    public String SINGLE_TX = ".singletx.";

    private List<BootstrapPropertiesService> services = new LinkedList<>();

    private class BootstrapProperty
    {

        private final BootstrapPropertiesService service;
        private final String key;

        private final String value;

        private BootstrapProperty(BootstrapPropertiesService service, String key, String value)
        {
            this.service = service;
            this.key = key;
            this.value = value;
        }

        public BootstrapPropertiesService getService()
        {
            return service;
        }

        public String getValue()
        {
            return value;
        }

        public String getKey()
        {
            return key;
        }

    }

    @Override
    public synchronized void bootstrap(MuleContext muleContext, BootstrapArtifactType bootstrapArtifactType) throws BootstrapException
    {
        // Merge and process properties
        List<BootstrapProperty> transformers = new LinkedList<>();
        List<BootstrapProperty> namedObjects = new LinkedList<>();
        List<BootstrapProperty> unnamedObjects = new LinkedList<>();
        List<BootstrapProperty> singleTransactionFactories = new LinkedList<>();

        for (BootstrapPropertiesService bootstrapPropertiesService : services)
        {
            Properties bootstrapProperties = bootstrapPropertiesService.getProperties();

            for (Map.Entry entry : bootstrapProperties.entrySet())
            {
                final String propertyKey = (String) entry.getKey();
                final String propertyValue = (String) entry.getValue();

                if (propertyKey.contains(OBJECT_KEY))
                {
                    final String newKey = createUniqueKey(propertyKey);
                    unnamedObjects.add(new BootstrapProperty(bootstrapPropertiesService, newKey, propertyValue));
                }
                else if (propertyKey.contains(TRANSFORMER_KEY))
                {
                    final String newKey = createUniqueKey(propertyKey);
                    transformers.add(new BootstrapProperty(bootstrapPropertiesService, newKey, propertyValue));
                }
                else if (propertyKey.contains(SINGLE_TX))
                {
                    if (!propertyKey.contains(".transaction.resource"))
                    {
                        String transactionResourceKey = propertyKey.replace(".transaction.factory", ".transaction.resource");
                        String transactionResource = bootstrapProperties.getProperty(transactionResourceKey);
                        if (transactionResource == null)
                        {
                            throw new BootstrapException(CoreMessages.createStaticMessage("There is no transaction resource specified for transaction factory %s", propertyKey), null);
                        }
                        singleTransactionFactories.add(new BootstrapProperty(bootstrapPropertiesService, propertyValue, transactionResource));
                    }
                }
                else
                {
                    namedObjects.add(new BootstrapProperty(bootstrapPropertiesService, propertyKey, propertyValue));
                }
            }
        }

        try
        {
            registerUnnamedObjects(unnamedObjects, muleContext.getRegistry(), bootstrapArtifactType);
            registerTransformers((MuleRegistryHelper) muleContext.getRegistry());
            registerTransformers(transformers, muleContext.getRegistry());
            registerObjects(namedObjects, muleContext.getRegistry(), bootstrapArtifactType);
            registerTransactionFactories(singleTransactionFactories, muleContext);
        }
        catch (Exception e1)
        {
            throw new BootstrapException(MessageFactory.createStaticMessage("Unable to bootstrap registry"), e1);
        }
    }

    private String createUniqueKey(String propertyKey)
    {
        return String.format("%s#%s", propertyKey.substring(0, propertyKey.lastIndexOf(".")), UUID.getUUID());
    }

    @Override
    public synchronized void register(BootstrapPropertiesService bootstrapPropertiesService)
    {
        services.add(bootstrapPropertiesService);
    }

    @Override
    public synchronized boolean unregister(BootstrapPropertiesService bootstrapPropertiesService)
    {
        return services.remove(bootstrapPropertiesService);
    }


    private void registerTransactionFactories(List<BootstrapProperty> singleTransactionFactories, MuleContext context) throws Exception
    {

        for (BootstrapProperty bootstrapProperty : singleTransactionFactories)
        {
            String transactionResourceClassNameProperties = (String) bootstrapProperty.getValue();
            String transactionFactoryClassName = (String) bootstrapProperty.getKey();
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
                Class<?> supportedType = bootstrapProperty.getService().forName(transactionResourceClassName);

                context.getTransactionFactoryManager().registerTransactionFactory(supportedType, (TransactionFactory) bootstrapProperty.getService().instantiateClass(transactionFactoryClassName));

            }
            catch (NoClassDefFoundError ncdfe)
            {
                throwExceptionIfNotOptional(optional, ncdfe, "Ignoring optional transaction factory: " + transactionResourceClassName);
            }
            catch (ClassNotFoundException cnfe)
            {
                throwExceptionIfNotOptional(optional, cnfe, "Ignoring optional transaction factory: " + transactionResourceClassName);
            }
        }
    }

    private void registerTransformers(List<BootstrapProperty> props, MuleRegistry registry) throws Exception
    {
        String transString;
        String name = null;
        String returnClassString;
        boolean optional = false;

        for (BootstrapProperty bootstrapProperty : props)
        {

            transString = (String) bootstrapProperty.getValue();
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
                    if (i > -1)
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
                        returnClass = bootstrapProperty.getService().forName(returnClassString);
                    }
                }
                Transformer trans = (Transformer) bootstrapProperty.service.instantiateClass(transClass);
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
                throwExceptionIfNotOptional(cause instanceof NoClassDefFoundError && optional, cause, "Ignoring optional transformer: " + transClass);
            }
            catch (NoClassDefFoundError ncdfe)
            {
                throwExceptionIfNotOptional(optional, ncdfe, "Ignoring optional transformer: " + transClass);

            }
            catch (ClassNotFoundException cnfe)
            {
                throwExceptionIfNotOptional(optional, cnfe, "Ignoring optional transformer: " + transClass);
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

    private void registerObjects(List<BootstrapProperty> boostrapProperties, Registry registry, BootstrapArtifactType bootstrapArtifactType) throws Exception
    {
        for (BootstrapProperty bootstrapProperty : boostrapProperties)
        {
            registerObject(bootstrapProperty.getService(), (String) bootstrapProperty.getKey(), (String) bootstrapProperty.getValue(), registry, bootstrapArtifactType);
        }
    }

    private void registerUnnamedObjects(List<BootstrapProperty> boostrapProperties, Registry registry, BootstrapArtifactType bootstrapArtifactType) throws Exception
    {
        for (BootstrapProperty bootstrapProperty : boostrapProperties)
        {
            registerObject(bootstrapProperty.getService(), bootstrapProperty.getKey(), (String) bootstrapProperty.getValue(), registry, bootstrapArtifactType);
        }
    }

    private void registerObject(BootstrapPropertiesService service, String key, String value, Registry registry, BootstrapArtifactType bootstrapArtifactType) throws Exception
    {
        BootstrapArtifactType bootstrapArtifactTypeParameterValue = BootstrapArtifactType.APP;

        boolean optional = false;
        String className = null;

        try
        {
            int x = value.indexOf(",");
            if (x > -1)
            {
                Properties p = PropertiesUtils.getPropertiesFromString(value.substring(x + 1), ',');
                if (p.containsKey(BootstrapArtifactType.APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY))
                {
                    bootstrapArtifactTypeParameterValue = BootstrapArtifactType.createFromString((String) p.get(BootstrapArtifactType.APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY));
                }
                optional = p.containsKey("optional");
                className = value.substring(0, x);
            }
            else
            {
                className = value;
            }

            if (!bootstrapArtifactTypeParameterValue.equals(BootstrapArtifactType.ALL) && !bootstrapArtifactTypeParameterValue.equals(bootstrapArtifactType))
            {
                return;
            }

            Object o = service.instantiateClass(className);
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
