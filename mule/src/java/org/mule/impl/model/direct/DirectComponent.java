/* 
* $Header$
* $Revision$
* $Date$
* ------------------------------------------------------------------------------------------------------
* 
* Copyright (c) SymphonySoft Limited. All rights reserved.
* http://www.symphonysoft.com
* 
* The software in this package is published under the terms of the BSD
* style license a copy of which has been included with this distribution in
* the LICENSE.txt file. 
*
*/
package org.mule.impl.model.direct;

import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.MuleMessage;
import org.mule.impl.RequestContext;
import org.mule.impl.model.AbstractComponent;
import org.mule.impl.model.DefaultMuleProxy;
import org.mule.impl.model.MuleProxy;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.model.UMOModel;
import org.mule.util.ClassHelper;
import org.apache.commons.beanutils.BeanUtils;

import java.util.List;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class DirectComponent extends AbstractComponent {

    protected List interceptorList = null;
    protected MuleProxy proxy;

    public DirectComponent(MuleDescriptor descriptor, UMOModel model) {
        super(descriptor, model);
    }

    protected void doInitialise() throws InitialisationException {

        try {
            Object component = create();
            proxy = new DefaultMuleProxy(component, descriptor, null);
            proxy.setStatistics(getStatistics());
        } catch (UMOException e) {
            throw new InitialisationException(e, this);
        }
    }

    protected UMOMessage doSend(UMOEvent event) throws UMOException {

        Object obj = proxy.onCall(event);
        if(obj instanceof UMOMessage) {
            return (UMOMessage)obj;
        } else {
            return new MuleMessage(obj, RequestContext.getProperties());
        }
    }

    protected void doDispatch(UMOEvent event) throws UMOException {
        proxy.onCall(event);
    }

    public Object create() throws UMOException
    {
        UMOManager manager = MuleManager.getInstance();
        Object impl = descriptor.getImplementation();
        Object component = null;

        if (impl instanceof String) {
            String reference = impl.toString();

            if (reference.startsWith(MuleDescriptor.IMPLEMENTATION_TYPE_LOCAL)) {
                String refName = reference.substring(MuleDescriptor.IMPLEMENTATION_TYPE_LOCAL.length());
                component = descriptor.getProperties().get(refName);
                if (component == null) {
                    throw new InitialisationException(new Message(Messages.NO_LOCAL_IMPL_X_SET_ON_DESCRIPTOR_X,
                                                                  refName,
                                                                  descriptor.getName()), this);
                }
            }

            if (component == null) {
                if (descriptor.isContainerManaged()) {
                    component = manager.getContainerContext().getComponent(reference);
                } else {
                    try {
                        component = ClassHelper.instanciateClass(reference, new Object[] {});
                    } catch (Exception e) {
                        throw new InitialisationException(new Message(Messages.CANT_INSTANCIATE_NON_CONTAINER_REF_X,
                                                                      reference), e, descriptor);
                    }
                }
            }
            if(descriptor.isSingleton()) descriptor.setImplementation(component);
        } else {
            component = impl;
        }

        try {
            BeanUtils.populate(component, descriptor.getProperties());
        } catch (Exception e) {
            throw new InitialisationException(new Message(Messages.FAILED_TO_SET_PROPERTIES_ON_X, "Component '"
                    + descriptor.getName() + "'"), e, descriptor);
        }
        // Call any custom initialisers
        descriptor.fireInitialisationCallbacks(component);

        return component;
    }
}
