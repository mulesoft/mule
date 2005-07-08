// Copyright (c) 2004-2005 Sun Microsystems Inc., All Rights Reserved.
/*
 * ServiceUnitManager.java
 *
 * SUN PROPRIETARY/CONFIDENTIAL.
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package javax.jbi.component;

import javax.jbi.management.DeploymentException;

/**
 * This interface defines component-supplied methods for managing service
 * unit deployments, and is implemented by the component. The JBI implementation
 * queries the component for the implementation of this interface using the
 * {@link Component#getServiceUnitManager()} method.
 *
 * @author JSR208 Expert Group
 */
public interface ServiceUnitManager
{
    /**
     * Deploy a Service Unit to the component. This is called by the JBI
     * implementation in order to deploy the given artifact to the implementing
     * component.
     * <p>
     * Upon successful deployment, a non-empty result string must be returned,
     * that starts with the JBI-defined component-task-result element.
     * For example:
     * <pre>
     * &lt;component-task-result&gt;
     *   &lt;component-name&gt;BC1&lt;/component-name&gt;
     *   &lt;component-task-result-details
     *     xmlns="http://java.sun.com/xml/ns/jbi/management-message"&gt;
     *       &lt;task-result-details&gt;
     *           &lt;task-id>deploy&lt;/task-id&gt;
     *           &lt;task-result>SUCCESS&lt;/task-result&gt;
     *       &lt;/task-result-details&gt;
     *   &lt;/component-task-result-details&gt;
     * &lt;/component-task-result&gt;
     * </pre>
     * A failed deployment of the service unit must be reported using the
     * <code>component-task-result</code> element as well; the
     * <code>task-result</code> must be set to FAILED.
     *
     * @param serviceUnitName name of the service unit being deployed; must be
     *        non-null and non-empty and unique among service units already
     *        deployed to the component.
     * @param serviceUnitRootPath path of the service unit artifact root, in
     *        platform specific format; must be non-null and non-empty.
     * @return a deployment status message, which is an XML string that conforms
     *         to the schema given in the <i>MBean Status and Result Strings</i>
     *         section of the <i><b>Management</b></i> chapter of the JBI
     *         specification; must be non-null and non-empty.
     * @exception DeploymentException if the deployment operation is
     *            unsuccessful.
     */
    String deploy(String serviceUnitName, String serviceUnitRootPath)
        throws DeploymentException;

    /**
     * Initialize the given deployed service unit. This is the first phase of
     * a two-phase start, where the component must prepare to receive service
     * requests related to the deployment (if any).
     * <p>
     * The serviceUnitRootPath parameter is provided to facilitate restart of
     * the component. This allows simple components to rely entirely on JBI's
     * ability to persist deployment information, avoiding the need for the
     * component to provide its own persistence mechanism.
     *
     * @param serviceUnitName name of the service unit being initialized; must
     *        be non-null, non-empty, and match the name of a previously
     *        deployed (but not yet undeployed) service unit.
     * @param serviceUnitRootPath path of the service unit artifact root, in
     *        platform specific format; must be non-null and non-empty.
     * @exception DeploymentException if the service unit is not deployed, or
     *            if it is in an incorrect state.
     */
    void init(String serviceUnitName, String serviceUnitRootPath)
        throws DeploymentException;

    /**
     * Start the deployed service unit. This is the second phase of a two-phase
     * start, where the component can now initiate service requests related to
     * the deployment.
     *
     * @param serviceUnitName the name of the service unit being started; must
     *        be non-null, non-empty, and match the name of a previously
     *        deployed (but not yet undeployed) service unit.
     * @exception DeploymentException if the service unit is not deployed, or
     *           if it is in an incorrect state.
     */
    void start(String serviceUnitName)
        throws DeploymentException;

    /**
     * Stop the deployed service unit. This causes the component to cease
     * generating service requests related to the given service unit. This
     * returns the service unit to a state equivalent to after
     * {@link #init(String, String)} was called.
     *
     * @param serviceUnitName name of the service unit being stopped; must
     *        be non-null, non-empty, and match the name of a previously
     *        deployed (but not yet undeployed) service unit.
     * @exception DeploymentException if the service unit is not deployed, or
     *            if it is in an incorrect state.
     */
    void stop(String serviceUnitName)
        throws DeploymentException;

    /**
     * Shut down the deployment. This causes the deployment to return to the
     * to the state it was in after {@link #deploy(String, String)}, and before
     * {@link #init(String, String)}.
     *
     * @param serviceUnitName name of the service unit being shut down; must
     *        be non-null, non-empty, and match the name of a previously
     *        deployed (but not yet undeployed) service unit.
     * @exception DeploymentException if the service unit is not deployed, or
     *            if it is in an incorrect state.
     */
    void shutDown(String serviceUnitName)
        throws DeploymentException;

    /**
     * Undeploy a service unit from the component. The service unit must be
     * shut down to undeploy it.
     *
     * @param serviceUnitName name of the service unit being undeployed; must
     *        be non-null, non-empty, and match the name of a previously
     *        deployed (but not yet undeployed) service unit.
     * @param serviceUnitRootPath path of the service unit artifact root, in
     *        platform specific format; must be non-null and non-empty.
     * @return deployment status message, which is an XML string that conforms
     *         to the <code>component-task-result</code> type from
     *         the schema given in the <i>MBean Status and Result Strings</i>
     *         section of the <i><b>Management</b></i> chapter of the JBI
     *         specification; must be non-null and non-empty.
     * @exception DeploymentException if undeployment operation is unsuccessful,
     *            or if the service unit is in an incorrect state.
     */
    String undeploy(String serviceUnitName, String serviceUnitRootPath)
        throws DeploymentException;
}
