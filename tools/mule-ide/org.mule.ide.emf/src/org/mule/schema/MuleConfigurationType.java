/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.schema;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.eclipse.emf.ecore.util.FeatureMap;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Configuration Type</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.mule.schema.MuleConfigurationType#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.MuleConfigurationType#getDescription <em>Description</em>}</li>
 *   <li>{@link org.mule.schema.MuleConfigurationType#getEnvironmentProperties <em>Environment Properties</em>}</li>
 *   <li>{@link org.mule.schema.MuleConfigurationType#getMuleEnvironmentProperties <em>Mule Environment Properties</em>}</li>
 *   <li>{@link org.mule.schema.MuleConfigurationType#getContainerContext <em>Container Context</em>}</li>
 *   <li>{@link org.mule.schema.MuleConfigurationType#getSecurityManager <em>Security Manager</em>}</li>
 *   <li>{@link org.mule.schema.MuleConfigurationType#getTransactionManager <em>Transaction Manager</em>}</li>
 *   <li>{@link org.mule.schema.MuleConfigurationType#getAgents <em>Agents</em>}</li>
 *   <li>{@link org.mule.schema.MuleConfigurationType#getConnector <em>Connector</em>}</li>
 *   <li>{@link org.mule.schema.MuleConfigurationType#getEndpointIdentifiers <em>Endpoint Identifiers</em>}</li>
 *   <li>{@link org.mule.schema.MuleConfigurationType#getTransformers <em>Transformers</em>}</li>
 *   <li>{@link org.mule.schema.MuleConfigurationType#getGlobalEndpoints <em>Global Endpoints</em>}</li>
 *   <li>{@link org.mule.schema.MuleConfigurationType#getInterceptorStack <em>Interceptor Stack</em>}</li>
 *   <li>{@link org.mule.schema.MuleConfigurationType#getModel <em>Model</em>}</li>
 *   <li>{@link org.mule.schema.MuleConfigurationType#getMuleDescriptor <em>Mule Descriptor</em>}</li>
 *   <li>{@link org.mule.schema.MuleConfigurationType#getId <em>Id</em>}</li>
 *   <li>{@link org.mule.schema.MuleConfigurationType#getVersion <em>Version</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.mule.schema.MulePackage#getMuleConfigurationType()
 * @model extendedMetaData="name='mule-configurationType' kind='mixed'"
 * @generated
 */
public interface MuleConfigurationType extends EObject {
	/**
	 * Returns the value of the '<em><b>Mixed</b></em>' attribute list.
	 * The list contents are of type {@link org.eclipse.emf.ecore.util.FeatureMap.Entry}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Mixed</em>' attribute list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Mixed</em>' attribute list.
	 * @see org.mule.schema.MulePackage#getMuleConfigurationType_Mixed()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true"
	 *        extendedMetaData="kind='elementWildcard' name=':mixed'"
	 * @generated
	 */
	FeatureMap getMixed();

	/**
	 * Returns the value of the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Description</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Description</em>' attribute.
	 * @see #setDescription(String)
	 * @see org.mule.schema.MulePackage#getMuleConfigurationType_Description()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='description' namespace='##targetNamespace'"
	 * @generated
	 */
	String getDescription();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleConfigurationType#getDescription <em>Description</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Description</em>' attribute.
	 * @see #getDescription()
	 * @generated
	 */
	void setDescription(String value);

	/**
	 * Returns the value of the '<em><b>Environment Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Environment Properties</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Environment Properties</em>' containment reference.
	 * @see #setEnvironmentProperties(EnvironmentPropertiesType)
	 * @see org.mule.schema.MulePackage#getMuleConfigurationType_EnvironmentProperties()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='environment-properties' namespace='##targetNamespace'"
	 * @generated
	 */
	EnvironmentPropertiesType getEnvironmentProperties();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleConfigurationType#getEnvironmentProperties <em>Environment Properties</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Environment Properties</em>' containment reference.
	 * @see #getEnvironmentProperties()
	 * @generated
	 */
	void setEnvironmentProperties(EnvironmentPropertiesType value);

	/**
	 * Returns the value of the '<em><b>Mule Environment Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Mule Environment Properties</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Mule Environment Properties</em>' containment reference.
	 * @see #setMuleEnvironmentProperties(MuleEnvironmentPropertiesType)
	 * @see org.mule.schema.MulePackage#getMuleConfigurationType_MuleEnvironmentProperties()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='mule-environment-properties' namespace='##targetNamespace'"
	 * @generated
	 */
	MuleEnvironmentPropertiesType getMuleEnvironmentProperties();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleConfigurationType#getMuleEnvironmentProperties <em>Mule Environment Properties</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Mule Environment Properties</em>' containment reference.
	 * @see #getMuleEnvironmentProperties()
	 * @generated
	 */
	void setMuleEnvironmentProperties(MuleEnvironmentPropertiesType value);

	/**
	 * Returns the value of the '<em><b>Container Context</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.ContainerContextType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Container Context</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Container Context</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getMuleConfigurationType_ContainerContext()
	 * @model type="org.mule.schema.ContainerContextType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='container-context' namespace='##targetNamespace'"
	 * @generated
	 */
	EList getContainerContext();

	/**
	 * Returns the value of the '<em><b>Security Manager</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Security Manager</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Security Manager</em>' containment reference.
	 * @see #setSecurityManager(SecurityManagerType)
	 * @see org.mule.schema.MulePackage#getMuleConfigurationType_SecurityManager()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='security-manager' namespace='##targetNamespace'"
	 * @generated
	 */
	SecurityManagerType getSecurityManager();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleConfigurationType#getSecurityManager <em>Security Manager</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Security Manager</em>' containment reference.
	 * @see #getSecurityManager()
	 * @generated
	 */
	void setSecurityManager(SecurityManagerType value);

	/**
	 * Returns the value of the '<em><b>Transaction Manager</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Transaction Manager</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Transaction Manager</em>' containment reference.
	 * @see #setTransactionManager(TransactionManagerType)
	 * @see org.mule.schema.MulePackage#getMuleConfigurationType_TransactionManager()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='transaction-manager' namespace='##targetNamespace'"
	 * @generated
	 */
	TransactionManagerType getTransactionManager();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleConfigurationType#getTransactionManager <em>Transaction Manager</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Transaction Manager</em>' containment reference.
	 * @see #getTransactionManager()
	 * @generated
	 */
	void setTransactionManager(TransactionManagerType value);

	/**
	 * Returns the value of the '<em><b>Agents</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Agents</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Agents</em>' containment reference.
	 * @see #setAgents(AgentsType)
	 * @see org.mule.schema.MulePackage#getMuleConfigurationType_Agents()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='agents' namespace='##targetNamespace'"
	 * @generated
	 */
	AgentsType getAgents();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleConfigurationType#getAgents <em>Agents</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Agents</em>' containment reference.
	 * @see #getAgents()
	 * @generated
	 */
	void setAgents(AgentsType value);

	/**
	 * Returns the value of the '<em><b>Connector</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.ConnectorType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Connector</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Connector</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getMuleConfigurationType_Connector()
	 * @model type="org.mule.schema.ConnectorType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='connector' namespace='##targetNamespace'"
	 * @generated
	 */
	EList getConnector();

	/**
	 * Returns the value of the '<em><b>Endpoint Identifiers</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Endpoint Identifiers</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Endpoint Identifiers</em>' containment reference.
	 * @see #setEndpointIdentifiers(EndpointIdentifiersType)
	 * @see org.mule.schema.MulePackage#getMuleConfigurationType_EndpointIdentifiers()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='endpoint-identifiers' namespace='##targetNamespace'"
	 * @generated
	 */
	EndpointIdentifiersType getEndpointIdentifiers();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleConfigurationType#getEndpointIdentifiers <em>Endpoint Identifiers</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Endpoint Identifiers</em>' containment reference.
	 * @see #getEndpointIdentifiers()
	 * @generated
	 */
	void setEndpointIdentifiers(EndpointIdentifiersType value);

	/**
	 * Returns the value of the '<em><b>Transformers</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Transformers</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Transformers</em>' containment reference.
	 * @see #setTransformers(TransformersType)
	 * @see org.mule.schema.MulePackage#getMuleConfigurationType_Transformers()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='transformers' namespace='##targetNamespace'"
	 * @generated
	 */
	TransformersType getTransformers();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleConfigurationType#getTransformers <em>Transformers</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Transformers</em>' containment reference.
	 * @see #getTransformers()
	 * @generated
	 */
	void setTransformers(TransformersType value);

	/**
	 * Returns the value of the '<em><b>Global Endpoints</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Global Endpoints</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Global Endpoints</em>' containment reference.
	 * @see #setGlobalEndpoints(GlobalEndpointsType)
	 * @see org.mule.schema.MulePackage#getMuleConfigurationType_GlobalEndpoints()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='global-endpoints' namespace='##targetNamespace'"
	 * @generated
	 */
	GlobalEndpointsType getGlobalEndpoints();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleConfigurationType#getGlobalEndpoints <em>Global Endpoints</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Global Endpoints</em>' containment reference.
	 * @see #getGlobalEndpoints()
	 * @generated
	 */
	void setGlobalEndpoints(GlobalEndpointsType value);

	/**
	 * Returns the value of the '<em><b>Interceptor Stack</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.InterceptorStackType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Interceptor Stack</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Interceptor Stack</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getMuleConfigurationType_InterceptorStack()
	 * @model type="org.mule.schema.InterceptorStackType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='interceptor-stack' namespace='##targetNamespace'"
	 * @generated
	 */
	EList getInterceptorStack();

	/**
	 * Returns the value of the '<em><b>Model</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Model</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Model</em>' containment reference.
	 * @see #setModel(ModelType)
	 * @see org.mule.schema.MulePackage#getMuleConfigurationType_Model()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='model' namespace='##targetNamespace'"
	 * @generated
	 */
	ModelType getModel();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleConfigurationType#getModel <em>Model</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Model</em>' containment reference.
	 * @see #getModel()
	 * @generated
	 */
	void setModel(ModelType value);

	/**
	 * Returns the value of the '<em><b>Mule Descriptor</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.MuleDescriptorType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Mule Descriptor</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Mule Descriptor</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getMuleConfigurationType_MuleDescriptor()
	 * @model type="org.mule.schema.MuleDescriptorType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='mule-descriptor' namespace='##targetNamespace'"
	 * @generated
	 */
	EList getMuleDescriptor();

	/**
	 * Returns the value of the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Id</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Id</em>' attribute.
	 * @see #setId(String)
	 * @see org.mule.schema.MulePackage#getMuleConfigurationType_Id()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='id' namespace='##targetNamespace'"
	 * @generated
	 */
	String getId();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleConfigurationType#getId <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Id</em>' attribute.
	 * @see #getId()
	 * @generated
	 */
	void setId(String value);

	/**
	 * Returns the value of the '<em><b>Version</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Version</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Version</em>' attribute.
	 * @see #setVersion(String)
	 * @see org.mule.schema.MulePackage#getMuleConfigurationType_Version()
	 * @model unique="false" dataType="org.mule.schema.VersionType" required="true"
	 *        extendedMetaData="kind='attribute' name='version' namespace='##targetNamespace'"
	 * @generated
	 */
	String getVersion();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleConfigurationType#getVersion <em>Version</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Version</em>' attribute.
	 * @see #getVersion()
	 * @generated
	 */
	void setVersion(String value);

} // MuleConfigurationType
