/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config.bootstrap;

import static org.apache.commons.lang3.exception.ExceptionUtils.getCause;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.ALL;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.transaction.TransactionFactory;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.util.PropertiesUtils;
import org.mule.runtime.core.api.config.bootstrap.BootstrapService;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for an object will load objects defined in a file called <code>registry-bootstrap.properties</code> into the local
 * registry. This allows modules and transports to make certain objects available by default. The most common use case is for a
 * module or transport to load stateless transformers into the registry. For this file to be located it must be present in the
 * modules META-INF directory under
 * 
 * <pre>
 * META-INF/org/mule/config/
 * </pre>
 * <p/>
 * The format of this file is a simple key / value pair. i.e.
 * 
 * <pre>
 * myobject = org.foo.MyObject
 * </pre>
 * 
 * Will register an instance of MyObject with a key of 'myobject'. If you don't care about the object name and want to ensure that
 * the ojbect gets a unique name you can use -
 * 
 * <pre>
 * object.1=org.foo.MyObject
 * object.2=org.bar.MyObject
 * </pre>
 * 
 * or
 * 
 * <pre>
 * myFoo=org.foo.MyObject
 * myBar=org.bar.MyObject
 * </pre>
 * 
 * It's also possible to define if the entry must be applied to a domain, an application, or both by using the parameter
 * applyToArtifactType.
 * 
 * <pre>
 * myFoo=org.foo.MyObject will be applied to any mule application since the parameter applyToArtifactType default value is app
 * myFoo=org.foo.MyObject;applyToArtifactType=app will be applied to any mule application
 * myFoo=org.foo.MyObject;applyToArtifactType=domain will be applied to any mule domain
 * myFoo=org.foo.MyObject;applyToArtifactType=app/domain will be applied to any mule application and any mule domain
 * </pre>
 * 
 * Loading transformers has a slightly different notation since you can define the 'returnClass' with optional mime type, and
 * 'name'of the transformer as parameters i.e.
 * 
 * <pre>
 * transformer.1=org.mule.compatibility.core.transport.jms.transformers.JMSMessageToObject,returnClass=byte[]
 * transformer.2=org.mule.compatibility.core.transport.jms.transformers.JMSMessageToObject,returnClass=java.lang.String:text/xml, name=JMSMessageToString
 * transformer.3=org.mule.compatibility.core.transport.jms.transformers.JMSMessageToObject,returnClass=java.util.Hashtable)
 * </pre>
 * 
 * Note that the key used for transformers must be 'transformer.x' where 'x' is a sequential number. The transformer name will be
 * automatically generated as JMSMessageToXXX where XXX is the return class name i.e. JMSMessageToString unless a 'name' parameter
 * is specified. If no 'returnClass' is specified the default in the transformer will be used.
 * <p/>
 * Note that all objects defined have to have a default constructor. They can implement injection interfaces such as
 * {@link org.mule.runtime.core.api.context.MuleContextAware} and lifecycle interfaces such as {@link Initialisable}.
 *
 * @since 3.7.0
 */
public abstract class AbstractRegistryBootstrap implements Initialisable {

  private static final String TRANSACTION_RESOURCE_SUFFIX = ".transaction.resource";
  private static final String OPTIONAL_ATTRIBUTE = "optional";
  private static final String RETURN_CLASS_PROPERTY = "returnClass";
  private static final String MIME_TYPE_PROPERTY = "mimeType";

  public static final String TRANSFORMER_KEY = ".transformer.";
  public static final String OBJECT_KEY = ".object.";
  public static final String SINGLE_TX = ".singletx.";

  protected ArtifactType artifactType = APP;
  protected final transient Logger logger = LoggerFactory.getLogger(getClass());
  protected MuleContext muleContext;

  /**
   * @param artifactType type of artifact. Bootstrap entries may be associated to an specific type of artifact. If it's not
   *        associated to the related artifact it will be ignored.
   * @param muleContext the {@code MuleContext} of the artifact.
   */
  public AbstractRegistryBootstrap(ArtifactType artifactType, MuleContext muleContext) {
    this.artifactType = artifactType;
    this.muleContext = muleContext;
  }

  /**
   * TODO Optimize me! MULE-9343
   * 
   * {@inheritDoc}
   */
  @Override
  public void initialise() throws InitialisationException {
    List<BootstrapService> bootstrapServices;
    try {
      bootstrapServices = muleContext.getRegistryBootstrapServiceDiscoverer().discover();
    } catch (Exception e) {
      throw new InitialisationException(e, this);
    }

    // Merge and process properties
    int objectCounter = 1;
    List<TransformerBootstrapProperty> transformers = new LinkedList<>();
    List<ObjectBootstrapProperty> namedObjects = new LinkedList<>();
    List<ObjectBootstrapProperty> unnamedObjects = new LinkedList<>();
    List<TransactionFactoryBootstrapProperty> singleTransactionFactories = new LinkedList<>();

    for (BootstrapService bootstrapService : bootstrapServices) {
      Properties bootstrapProperties = bootstrapService.getProperties();

      for (Map.Entry entry : bootstrapProperties.entrySet()) {
        final String propertyKey = (String) entry.getKey();
        final String propertyValue = (String) entry.getValue();

        if (propertyKey.contains(OBJECT_KEY)) {
          String newKey = propertyKey.substring(0, propertyKey.lastIndexOf(".")) + objectCounter++;
          unnamedObjects.add(createObjectBootstrapProperty(bootstrapService, newKey, propertyValue));
        } else if (propertyKey.contains(TRANSFORMER_KEY)) {
          transformers.add(createTransformerBootstrapProperty(bootstrapService, propertyValue));
        } else if (propertyKey.contains(SINGLE_TX)) {
          if (!propertyKey.contains(TRANSACTION_RESOURCE_SUFFIX)) {
            singleTransactionFactories.add(createTransactionFactoryBootstrapProperty(bootstrapService, bootstrapProperties,
                                                                                     propertyKey, propertyValue));
          }
        } else {
          namedObjects.add(createObjectBootstrapProperty(bootstrapService, propertyKey, propertyValue));
        }
      }
    }

    try {
      registerUnnamedObjects(unnamedObjects);
      registerTransformers();
      registerTransformers(transformers);
      registerObjects(namedObjects);
      registerTransactionFactories(singleTransactionFactories, muleContext);
    } catch (Exception e1) {
      throw new InitialisationException(e1, this);
    }
  }

  private TransformerBootstrapProperty createTransformerBootstrapProperty(BootstrapService bootstrapService,
                                                                          String propertyValue) {
    String transString;
    String name = null;
    String returnClassName;
    boolean optional = false;
    transString = propertyValue;
    returnClassName = null;
    int index = transString.indexOf(",");
    if (index > -1) {
      Properties p = PropertiesUtils.getPropertiesFromString(transString.substring(index + 1), ',');
      name = p.getProperty("name", null);
      returnClassName = p.getProperty("returnClass", null);
      optional = p.containsKey(OPTIONAL_ATTRIBUTE);
    }
    String mime = null;

    if (returnClassName != null) {
      int i = returnClassName.indexOf(":");
      if (i > -1) {
        mime = returnClassName.substring(i + 1);
        returnClassName = returnClassName.substring(0, i);
      }
    }

    final String className = index == -1 ? transString : transString.substring(0, index);

    final Map<String, String> properties = new HashMap<>();
    properties.put(MIME_TYPE_PROPERTY, mime);
    properties.put(RETURN_CLASS_PROPERTY, returnClassName);

    return new TransformerBootstrapProperty(bootstrapService, APP, optional, name, className, returnClassName, mime);
  }

  private TransactionFactoryBootstrapProperty createTransactionFactoryBootstrapProperty(BootstrapService bootstrapService,
                                                                                        Properties bootstrapProperties,
                                                                                        String propertyKey, String propertyValue)
      throws InitialisationException {
    String transactionResourceKey = propertyKey.replace(".transaction.factory", TRANSACTION_RESOURCE_SUFFIX);
    String transactionResource = bootstrapProperties.getProperty(transactionResourceKey);
    if (transactionResource == null) {
      throw new InitialisationException(CoreMessages.createStaticMessage(String
          .format("There is no transaction resource specified for transaction factory %s", propertyKey)), this);
    }

    String transactionResourceClassNameProperties = transactionResource;
    boolean optional = false;
    int index = transactionResourceClassNameProperties.indexOf(",");
    if (index > -1) {
      Properties p = PropertiesUtils.getPropertiesFromString(transactionResourceClassNameProperties.substring(index + 1), ',');
      optional = p.containsKey(OPTIONAL_ATTRIBUTE);
    }
    final String transactionResourceClassName =
        (index == -1 ? transactionResourceClassNameProperties : transactionResourceClassNameProperties.substring(0, index));

    return new TransactionFactoryBootstrapProperty(bootstrapService, APP, optional, propertyValue, transactionResourceClassName);
  }

  private ObjectBootstrapProperty createObjectBootstrapProperty(BootstrapService bootstrapService, String propertyKey,
                                                                String propertyValue) {
    boolean optional = false;
    String className;
    ArtifactType artifactTypeParameterValue = APP;

    final String value = propertyValue;
    int index = value.indexOf(",");
    if (index > -1) {
      Properties p = PropertiesUtils.getPropertiesFromString(value.substring(index + 1), ',');
      if (p.containsKey(ArtifactType.APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY)) {
        artifactTypeParameterValue =
            ArtifactType.createFromString((String) p.get(ArtifactType.APPLY_TO_ARTIFACT_TYPE_PARAMETER_KEY));
      }
      optional = p.containsKey(OPTIONAL_ATTRIBUTE);
      className = value.substring(0, index);
    } else {
      className = value;
    }

    return new ObjectBootstrapProperty(bootstrapService, artifactTypeParameterValue, optional, propertyKey, className);
  }

  private void registerUnnamedObjects(List<ObjectBootstrapProperty> bootstrapProperties) throws Exception {
    for (ObjectBootstrapProperty bootstrapProperty : bootstrapProperties) {
      registerObject(bootstrapProperty);
    }
  }

  private void registerObjects(List<ObjectBootstrapProperty> bootstrapProperties) throws Exception {
    for (ObjectBootstrapProperty bootstrapProperty : bootstrapProperties) {
      registerObject(bootstrapProperty);
    }
  }

  private void registerObject(ObjectBootstrapProperty bootstrapProperty) throws Exception {
    try {
      if (!bootstrapProperty.getArtifactType().equals(ALL) && !bootstrapProperty.getArtifactType().equals(artifactType)) {
        return;
      }

      doRegisterObject(bootstrapProperty);
    } catch (InvocationTargetException e) {
      Throwable cause = getCause(e);
      throwExceptionIfNotOptional(cause instanceof NoClassDefFoundError && bootstrapProperty.getOptional(), cause,
                                  bootstrapProperty);
    } catch (NoClassDefFoundError | ClassNotFoundException | NoSuchMethodException e) {
      throwExceptionIfNotOptional(bootstrapProperty.getOptional(), e, bootstrapProperty);
    }
  }

  private void registerTransactionFactories(List<TransactionFactoryBootstrapProperty> singleTransactionFactories,
                                            MuleContext context)
      throws Exception {
    for (TransactionFactoryBootstrapProperty bootstrapProperty : singleTransactionFactories) {
      try {
        final Class<?> supportedType =
            bootstrapProperty.getService().forName(bootstrapProperty.getTransactionResourceClassName());
        context.getTransactionFactoryManager().registerTransactionFactory(supportedType, (TransactionFactory) bootstrapProperty
            .getService().instantiateClass(bootstrapProperty.getTransactionFactoryClassName()));
      } catch (NoClassDefFoundError | ClassNotFoundException ncdfe) {
        throwExceptionIfNotOptional(bootstrapProperty.getOptional(), ncdfe, bootstrapProperty);
      }
    }
  }

  private void registerTransformers(List<TransformerBootstrapProperty> props) throws Exception {
    for (TransformerBootstrapProperty bootstrapProperty : props) {
      try {
        final Class<? extends Transformer> transformerClass =
            bootstrapProperty.getService().forName(bootstrapProperty.getClassName());

        Class<?> returnClass = null;
        String returnClassString = bootstrapProperty.getReturnClassName();
        if (returnClassString != null) {
          if (returnClassString.equals("byte[]")) {
            returnClass = byte[].class;
          } else {
            returnClass = bootstrapProperty.getService().forName(returnClassString);
          }
        }

        doRegisterTransformer(bootstrapProperty, returnClass, transformerClass);
      } catch (InvocationTargetException e) {
        Throwable cause = getCause(e);
        throwExceptionIfNotOptional(cause instanceof NoClassDefFoundError && bootstrapProperty.getOptional(), cause,
                                    bootstrapProperty);
      } catch (NoClassDefFoundError | ClassNotFoundException e) {
        throwExceptionIfNotOptional(bootstrapProperty.getOptional(), e, bootstrapProperty);
      }
    }
  }

  protected abstract void doRegisterTransformer(TransformerBootstrapProperty bootstrapProperty, Class<?> returnClass,
                                                Class<? extends Transformer> transformerClass)
      throws Exception;

  protected abstract void registerTransformers() throws MuleException;

  protected abstract void doRegisterObject(ObjectBootstrapProperty bootstrapProperty) throws Exception;

  private void throwExceptionIfNotOptional(boolean optional, Throwable t, AbstractBootstrapProperty bootstrapProperty)
      throws Exception {
    if (optional) {
      if (logger.isDebugEnabled()) {
        logger.debug("Ignoring optional " + bootstrapProperty);
      }
    } else if (t instanceof Exception) {
      throw (Exception) t;
    } else {
      throw new Exception(t);
    }
  }

}
