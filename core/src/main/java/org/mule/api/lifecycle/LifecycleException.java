/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.lifecycle;

import org.mule.api.MuleException;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.util.ObjectUtils;

/** <code>LifecycleException</code> TODO */

public class LifecycleException extends MuleException
{

    /** Serial version */
    private static final long serialVersionUID = 2909614055858287394L;

    private transient Object component;

    /**
     * @param message   the exception message
     * @param component the object that failed during a lifecycle method call
     */
    public LifecycleException(Message message, Object component)
    {
        super(message);
        this.component = component;
    }

    /**
     * @param message   the exception message
     * @param cause     the exception that cause this exception to be thrown
     * @param component the object that failed during a lifecycle method call
     */
    public LifecycleException(Message message, Throwable cause, Object component)
    {
        super(message, cause);
        this.component = component;
    }

    /**
     * @param cause     the exception that cause this exception to be thrown
     * @param component the object that failed during a lifecycle method call
     */
    public LifecycleException(Throwable cause, Object component)
    {
        super(CoreMessages.createStaticMessage(cause.getMessage()), cause);
        this.component = component;
        addInfo("Object", ObjectUtils.toString(component, "null"));
    }

    public Object getComponent()
    {
        return component;
    }
}
