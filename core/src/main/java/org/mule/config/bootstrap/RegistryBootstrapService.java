/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.bootstrap;

import org.mule.api.MuleContext;

/**
 * Bootstraps a {@link MuleContext} using registered {@link BootstrapPropertiesService}
 *
 * <p/>
 * Properties will be managed differently depending on the key format.
 * Will register an instance of MyObject with a key of 'myobject'. If you don't care about the object name and want to
 * ensure that the object gets a unique name you can use -
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
 */
public interface RegistryBootstrapService
{

    /**
     * Bootstrap a {@link MuleContext}
     *
     * @param muleContext context to bootstrap
     * @param bootstrapArtifactType type of artifact to include during bootstrapping
     * @throws BootstrapException if bootstrapping fails
     */
    void bootstrap(MuleContext muleContext, BootstrapArtifactType bootstrapArtifactType) throws BootstrapException;

    /**
     * Registers a {@link BootstrapPropertiesService}
     *
     * @param bootstrapPropertiesService service to register. Non null.
     */
    void register(BootstrapPropertiesService bootstrapPropertiesService);

    /**
     * Unregister a {@link BootstrapPropertiesService}
     *
     * @param bootstrapPropertiesService service to unregister
     * @return true if the service was unregister, false if the service was not registered
     */
    boolean unregister(BootstrapPropertiesService bootstrapPropertiesService);
}
