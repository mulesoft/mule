// Copyright (c) 2004-2005 Sun Microsystems Inc., All Rights Reserved.
/*
 * DeploymentServiceMBean.java
 *
 * SUN PROPRIETARY/CONFIDENTIAL.
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package javax.jbi.management;

/**
 * The deployment service MBean allows administrative tools to manage service
 * assembly deployments. The tasks supported are:
 * <ul>
 *   <li>Deploying a service assembly.</li>
 *   <li>Undeploying a previously deployed service assembly.</li>
 *   <li>Querying deployed service assemblies:
 *     <ul>
 *       <li>For all components in the system.</li>
 *       <li>For a particular component.</li>
 *     </ul>
 *   </li>
 *   <li>Control the state of deployed service assemblies:
 *     <ul>
 *       <li>Start the service units that contained in the SA.</li>
 *       <li>Stop the service units that contained in the SA. </li>
 *       <li>Shut down the service units that contained in the SA.</li>
 *     </ul>
 *   </li>
 *   <li>Query the service units deployed to a particular component.</li>
 *   <li>Check if a service unit is deployed to a particular component.</li>
 *   <li>Query the deployment descriptor for a particular service assembly.</li>
 * </ul>
 *
 * @author JSR208 Expert Group
 */
public interface DeploymentServiceMBean
{
    
    /**
     * Deploys the given SA to the JBI environment.
     * @param saZipURL  String containing the location
     *                of the Service Assembly zip file.
     * @return Result/Status of the SA deployment.
     * @throws Exception if complete deployment fails.
     *
     */
    String deploy (String saZipURL) throws Exception;
    
    
    /**
     * Undeploys the given SA from the JBI environment.
     * @param saName  name of the SA that has to be undeployed.
     * @return Result/Status of the SA undeployment.
     * @throws Exception if compelete undeployment fails.
     */
    String undeploy (String saName) throws Exception;
    
       
    /**
     * Returns a list of Service Units that are currently deployed to
     * the given component.
     * @param componentName name of the component. 
     * @return List of deployed ASA Ids.
     * @throws Exception if unable to retrieve service unit list.
     *
     */
    String[] getDeployedServiceUnitList (String componentName) throws Exception;
    
    
    /**
     * Returns a list of Service Assemblies deployed to the JBI enviroment.
     * @return list of Service Assembly Name's.
     * @throws Exception if unable to retrieve service assembly list.
     *
     */
    String[] getDeployedServiceAssemblies () throws Exception;
    
    
    /**
     * Returns the descriptor of the Service Assembly that
     * was deployed to the JBI enviroment.
     * @param saName name of the service assembly.
     * @return descriptor of the Service Assembly.
     * @throws Exception if unable to retrieve descriptor.
     *
     */
    String getServiceAssemblyDescriptor (String saName) throws Exception;
   
    
    /**
     * Returns a list of Service Assemblies that contain SUs
     *     for the given component.
     * @param componentName name of the component.
     * @return list of Service Assembly names.
     * @throws Exception if unable to retrieve service assembly list.
     *
     */
    String[] getDeployedServiceAssembliesForComponent (String componentName)
        throws Exception;
    
    
    /**
     * Returns a list of components(to which SUs are targeted for)
     * in a Service Assembly.
     * @param saName name of the service assembly.
     * @return list of component names.
     * @throws Exception if unable to retrieve component list.
     *
     */
    String[] getComponentsForDeployedServiceAssembly (String saName) throws Exception;
    
    
    /**
     * Returns a boolean value indicating whether
     * the SU is currently deployed.
     * @param componentName - name of component.
     * @param suName - name of the Service Unit.
     * @return boolean value indicating whether the SU
     *                 is currently deployed.
     * @throws Exception if unable to return status of service unit.
     */
    boolean isDeployedServiceUnit (String componentName, String suName) throws Exception;
    
    
    /**
     * Returns a boolean value indicating whether
     * the SU can be deployed to a component.
     * @param componentName - name of the component.
     * @return boolean value indicating whether the SU
     *                 can be deployed.
     *
     */
    boolean canDeployToComponent (String componentName);
    
    
    /**
     * Starts the service assembly and puts it in 
     * RUNNING state. 
     * @param serviceAssemblyName - name of the service 
     *                              assembly.
     * @return Result/Status of this operation.
     * @throws Exception if operation fails.
     *
     */
    String start(String serviceAssemblyName) throws Exception;
    
    /**
     * Stops the service assembly and puts it in 
     * STOPPED state. 
     * @param serviceAssemblyName - name of the service 
     *                              assembly.
     * @return Result/Status of this operation.
     * @throws Exception if operation fails.
     *
     */
    String stop(String serviceAssemblyName) throws Exception;
    
    /**
     * Shutdown the service assembly and puts it in 
     * SHUTDOWN state. 
     * @param serviceAssemblyName - name of the service 
     *                              assembly.
     * @return Result/Status of this operation.
     * @throws Exception if operation fails.
     *
     */
    String shutDown(String serviceAssemblyName) throws Exception;
    
    /**
     * Returns the state of service assembly.
     *
     * @param serviceAssemblyName - name of the service 
     *                              assembly.
     * @return State of the service assembly.
     * @throws Exception if operation fails.
     *
     */
    String getState(String serviceAssemblyName) throws Exception;

    /** The service assembly is started. This means that the assembly's offered
     *  services can accept message exchanges, and it can send exchanges to 
     *  consume services. */
    static final String STARTED   = "Started";
    
     /** The service assembly has been deployed, or shutdown */
    static final String SHUTDOWN = "Shutdown";

    /** The service assembly is stopped. This means that the assembly's offered
     *  services can accept message exchanges, but it will not send any. */
    static final String STOPPED   = "Stopped";
}
