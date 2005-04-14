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
 */
package org.mule.impl.container;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.RecoverableException;
import org.mule.umo.manager.ContainerException;
import org.mule.umo.manager.ObjectNotFoundException;

import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import java.io.Reader;
import java.util.Hashtable;
import java.util.Map;

/**
 * <code>JndiContainerContext</code> is a container implementaiton that exposes
 * a jndi context.  What ever properties are set on the container in configuration
 * will be passed to the initial context.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JndiContainerContext extends AbstractContainerContext
{
    private InitialContext initialContext;
    private Map environment;

    public JndiContainerContext() {
        super("jndi");
    }

    public Map getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map environment) {
        this.environment = environment;
    }

    public Object getComponent(Object key) throws ObjectNotFoundException
    {
        try {
            if(key instanceof Name) {
                return initialContext.lookup((Name)key);
            } else {
                return initialContext.lookup(key.toString());
            }
        } catch (NamingException e) {
            throw new ObjectNotFoundException(key.toString(), e);
        }
    }

    public void configure(Reader configuration) throws ContainerException
    {
        throw new UnsupportedOperationException("configure(Reader)");
    }

    public void initialise() throws InitialisationException, RecoverableException
    {
        try {
            initialContext = new InitialContext(new Hashtable(environment));
        } catch (NamingException e) {
            throw new InitialisationException(new Message(Messages.FAILED_TO_CREATE_X, "Jndi context"), e);
        }
    }
}
