// Copyright (c) 2004-2005 Sun Microsystems Inc., All Rights Reserved.
/*
 * ComponentLifeCycle.java
 *
 * SUN PROPRIETARY/CONFIDENTIAL.
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package javax.jbi.component;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;

import javax.management.ObjectName;

/**
 * This interface must be implemented by a JBI component to provide 
 * initialization, start, stop, and shutdown life cycle processing. These 
 * methods comprise the life cycle contract between the JBI implementation and 
 * the component. The life cycle of a component begins with a call to the init()
 * method on an instance of the component's implementation of this interface, 
 * and ends with the first call to the shutDown() method on that instance. 
 * Between these two calls, there can be any number of stop() and start() calls.
 * <p>
 * The JBI implementation must track the running state of a component, and
 * ensure that life cycle state changes are always legal. For example, if
 * the management interface for controlling a component's life cycle
 * ({@link javax.jbi.management.ComponentLifeCycleMBean}) is used to start
 * a component that was just installed (and thus in the <i>Shutdown</i> state),
 * the implementation must invoke this component's 
 * {@link #init(ComponentContext)} method before invoking its 
 * {@link #start()} method.
 *
 * @author JSR208 Expert Group
 */
public interface ComponentLifeCycle
{
    /**
     * Get the JMX object name for the extension MBean for this component; if
     * there is none, return <code>null</code>.
     *
     * @return the JMX object name of the additional MBean or <code>null</code>
     * if there is no additional MBean.
     */
    ObjectName getExtensionMBeanName();

    /**
     * Initialize the component. This performs initialization required by the 
     * component but does not make it ready to process messages. This method is 
     * called once for each life cycle of the component.
     * <p>
     * If the component needs to register an additional MBean to extend its
     * life cycle, or provide other component management tasks, it should
     * be registered during this call.
     * 
     * @param context the component's context, providing access to component
     *                data provided by the JBI environment; must be non-null.
     * @exception JBIException if the component is unable to initialize.
     */
    void init(ComponentContext context)
        throws JBIException;

    /**
     * Shut down the component. This performs clean-up, releasing all run-time
     * resources used by the component. Once this method has been called, 
     * {@link #init(ComponentContext)} must be called before the component can 
     * be started again with a call to {@link #start()}.
     *
     * @exception JBIException if the component is unable to shut down.
     */
    void shutDown()
        throws JBIException;

    /**
     * Start the component. This makes the component ready to process messages. 
     * This method is called after {@link #init(ComponentContext)}, both when
     * the component is being started for the first time and when the component
     * is being restarted after a previous call to {@link #shutDown()}.
     * If {@link #stop()} was called previously but {@link #shutDown()} was not,
     * <code>start()</code> can be called again without another call to
     * {@link #init(ComponentContext)}.
     * 
     * @exception JBIException if the component is unable to start.
     */
    void start()
        throws JBIException;

    /**
     * Stop the component. This makes the component stop accepting messages for 
     * processing. After a call to this method, {@link #start()} may be called
     * again without first calling {@link #init(ComponentContext)}.
     * 
     * @exception JBIException if the component is unable to stop.
     */
    void stop()
        throws JBIException;
}
