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
 * A representation of the model object '<em><b>Descriptor Type</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.mule.schema.MuleDescriptorType#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.MuleDescriptorType#getInboundRouter <em>Inbound Router</em>}</li>
 *   <li>{@link org.mule.schema.MuleDescriptorType#getOutboundRouter <em>Outbound Router</em>}</li>
 *   <li>{@link org.mule.schema.MuleDescriptorType#getResponseRouter <em>Response Router</em>}</li>
 *   <li>{@link org.mule.schema.MuleDescriptorType#getInterceptor <em>Interceptor</em>}</li>
 *   <li>{@link org.mule.schema.MuleDescriptorType#getThreadingProfile <em>Threading Profile</em>}</li>
 *   <li>{@link org.mule.schema.MuleDescriptorType#getPoolingProfile <em>Pooling Profile</em>}</li>
 *   <li>{@link org.mule.schema.MuleDescriptorType#getQueueProfile <em>Queue Profile</em>}</li>
 *   <li>{@link org.mule.schema.MuleDescriptorType#getExceptionStrategy <em>Exception Strategy</em>}</li>
 *   <li>{@link org.mule.schema.MuleDescriptorType#getProperties <em>Properties</em>}</li>
 *   <li>{@link org.mule.schema.MuleDescriptorType#isContainerManaged <em>Container Managed</em>}</li>
 *   <li>{@link org.mule.schema.MuleDescriptorType#getImplementation <em>Implementation</em>}</li>
 *   <li>{@link org.mule.schema.MuleDescriptorType#getInboundEndpoint <em>Inbound Endpoint</em>}</li>
 *   <li>{@link org.mule.schema.MuleDescriptorType#getInboundTransformer <em>Inbound Transformer</em>}</li>
 *   <li>{@link org.mule.schema.MuleDescriptorType#getInitialState <em>Initial State</em>}</li>
 *   <li>{@link org.mule.schema.MuleDescriptorType#getName <em>Name</em>}</li>
 *   <li>{@link org.mule.schema.MuleDescriptorType#getOutboundEndpoint <em>Outbound Endpoint</em>}</li>
 *   <li>{@link org.mule.schema.MuleDescriptorType#getOutboundTransformer <em>Outbound Transformer</em>}</li>
 *   <li>{@link org.mule.schema.MuleDescriptorType#getRef <em>Ref</em>}</li>
 *   <li>{@link org.mule.schema.MuleDescriptorType#getResponseTransformer <em>Response Transformer</em>}</li>
 *   <li>{@link org.mule.schema.MuleDescriptorType#isSingleton <em>Singleton</em>}</li>
 *   <li>{@link org.mule.schema.MuleDescriptorType#getVersion <em>Version</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.mule.schema.MulePackage#getMuleDescriptorType()
 * @model extendedMetaData="name='mule-descriptorType' kind='mixed'"
 * @generated
 */
public interface MuleDescriptorType extends EObject {
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
	 * @see org.mule.schema.MulePackage#getMuleDescriptorType_Mixed()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true"
	 *        extendedMetaData="kind='elementWildcard' name=':mixed'"
	 * @generated
	 */
	FeatureMap getMixed();

	/**
	 * Returns the value of the '<em><b>Inbound Router</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Inbound Router</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Inbound Router</em>' containment reference.
	 * @see #setInboundRouter(InboundRouterType)
	 * @see org.mule.schema.MulePackage#getMuleDescriptorType_InboundRouter()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='inbound-router' namespace='##targetNamespace'"
	 * @generated
	 */
	InboundRouterType getInboundRouter();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleDescriptorType#getInboundRouter <em>Inbound Router</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Inbound Router</em>' containment reference.
	 * @see #getInboundRouter()
	 * @generated
	 */
	void setInboundRouter(InboundRouterType value);

	/**
	 * Returns the value of the '<em><b>Outbound Router</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Outbound Router</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Outbound Router</em>' containment reference.
	 * @see #setOutboundRouter(OutboundRouterType)
	 * @see org.mule.schema.MulePackage#getMuleDescriptorType_OutboundRouter()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='outbound-router' namespace='##targetNamespace'"
	 * @generated
	 */
	OutboundRouterType getOutboundRouter();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleDescriptorType#getOutboundRouter <em>Outbound Router</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Outbound Router</em>' containment reference.
	 * @see #getOutboundRouter()
	 * @generated
	 */
	void setOutboundRouter(OutboundRouterType value);

	/**
	 * Returns the value of the '<em><b>Response Router</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Response Router</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Response Router</em>' containment reference.
	 * @see #setResponseRouter(ResponseRouterType)
	 * @see org.mule.schema.MulePackage#getMuleDescriptorType_ResponseRouter()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='response-router' namespace='##targetNamespace'"
	 * @generated
	 */
	ResponseRouterType getResponseRouter();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleDescriptorType#getResponseRouter <em>Response Router</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Response Router</em>' containment reference.
	 * @see #getResponseRouter()
	 * @generated
	 */
	void setResponseRouter(ResponseRouterType value);

	/**
	 * Returns the value of the '<em><b>Interceptor</b></em>' containment reference list.
	 * The list contents are of type {@link org.mule.schema.InterceptorType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Interceptor</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Interceptor</em>' containment reference list.
	 * @see org.mule.schema.MulePackage#getMuleDescriptorType_Interceptor()
	 * @model type="org.mule.schema.InterceptorType" containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='interceptor' namespace='##targetNamespace'"
	 * @generated
	 */
	EList getInterceptor();

	/**
	 * Returns the value of the '<em><b>Threading Profile</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Threading Profile</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Threading Profile</em>' containment reference.
	 * @see #setThreadingProfile(ThreadingProfileType)
	 * @see org.mule.schema.MulePackage#getMuleDescriptorType_ThreadingProfile()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='threading-profile' namespace='##targetNamespace'"
	 * @generated
	 */
	ThreadingProfileType getThreadingProfile();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleDescriptorType#getThreadingProfile <em>Threading Profile</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Threading Profile</em>' containment reference.
	 * @see #getThreadingProfile()
	 * @generated
	 */
	void setThreadingProfile(ThreadingProfileType value);

	/**
	 * Returns the value of the '<em><b>Pooling Profile</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Pooling Profile</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Pooling Profile</em>' containment reference.
	 * @see #setPoolingProfile(PoolingProfileType)
	 * @see org.mule.schema.MulePackage#getMuleDescriptorType_PoolingProfile()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='pooling-profile' namespace='##targetNamespace'"
	 * @generated
	 */
	PoolingProfileType getPoolingProfile();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleDescriptorType#getPoolingProfile <em>Pooling Profile</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Pooling Profile</em>' containment reference.
	 * @see #getPoolingProfile()
	 * @generated
	 */
	void setPoolingProfile(PoolingProfileType value);

	/**
	 * Returns the value of the '<em><b>Queue Profile</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Queue Profile</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Queue Profile</em>' containment reference.
	 * @see #setQueueProfile(QueueProfileType)
	 * @see org.mule.schema.MulePackage#getMuleDescriptorType_QueueProfile()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='queue-profile' namespace='##targetNamespace'"
	 * @generated
	 */
	QueueProfileType getQueueProfile();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleDescriptorType#getQueueProfile <em>Queue Profile</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Queue Profile</em>' containment reference.
	 * @see #getQueueProfile()
	 * @generated
	 */
	void setQueueProfile(QueueProfileType value);

	/**
	 * Returns the value of the '<em><b>Exception Strategy</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Exception Strategy</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Exception Strategy</em>' containment reference.
	 * @see #setExceptionStrategy(ExceptionStrategyType)
	 * @see org.mule.schema.MulePackage#getMuleDescriptorType_ExceptionStrategy()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='exception-strategy' namespace='##targetNamespace'"
	 * @generated
	 */
	ExceptionStrategyType getExceptionStrategy();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleDescriptorType#getExceptionStrategy <em>Exception Strategy</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Exception Strategy</em>' containment reference.
	 * @see #getExceptionStrategy()
	 * @generated
	 */
	void setExceptionStrategy(ExceptionStrategyType value);

	/**
	 * Returns the value of the '<em><b>Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Properties</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Properties</em>' containment reference.
	 * @see #setProperties(PropertiesType)
	 * @see org.mule.schema.MulePackage#getMuleDescriptorType_Properties()
	 * @model containment="true" resolveProxies="false" transient="true" volatile="true" derived="true"
	 *        extendedMetaData="kind='element' name='properties' namespace='##targetNamespace'"
	 * @generated
	 */
	PropertiesType getProperties();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleDescriptorType#getProperties <em>Properties</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Properties</em>' containment reference.
	 * @see #getProperties()
	 * @generated
	 */
	void setProperties(PropertiesType value);

	/**
	 * Returns the value of the '<em><b>Container Managed</b></em>' attribute.
	 * The default value is <code>"true"</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Container Managed</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Container Managed</em>' attribute.
	 * @see #isSetContainerManaged()
	 * @see #unsetContainerManaged()
	 * @see #setContainerManaged(boolean)
	 * @see org.mule.schema.MulePackage#getMuleDescriptorType_ContainerManaged()
	 * @model default="true" unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.Boolean"
	 *        extendedMetaData="kind='attribute' name='containerManaged' namespace='##targetNamespace'"
	 * @generated
	 */
	boolean isContainerManaged();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleDescriptorType#isContainerManaged <em>Container Managed</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Container Managed</em>' attribute.
	 * @see #isSetContainerManaged()
	 * @see #unsetContainerManaged()
	 * @see #isContainerManaged()
	 * @generated
	 */
	void setContainerManaged(boolean value);

	/**
	 * Unsets the value of the '{@link org.mule.schema.MuleDescriptorType#isContainerManaged <em>Container Managed</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetContainerManaged()
	 * @see #isContainerManaged()
	 * @see #setContainerManaged(boolean)
	 * @generated
	 */
	void unsetContainerManaged();

	/**
	 * Returns whether the value of the '{@link org.mule.schema.MuleDescriptorType#isContainerManaged <em>Container Managed</em>}' attribute is set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return whether the value of the '<em>Container Managed</em>' attribute is set.
	 * @see #unsetContainerManaged()
	 * @see #isContainerManaged()
	 * @see #setContainerManaged(boolean)
	 * @generated
	 */
	boolean isSetContainerManaged();

	/**
	 * Returns the value of the '<em><b>Implementation</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Implementation</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Implementation</em>' attribute.
	 * @see #setImplementation(String)
	 * @see org.mule.schema.MulePackage#getMuleDescriptorType_Implementation()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
	 *        extendedMetaData="kind='attribute' name='implementation' namespace='##targetNamespace'"
	 * @generated
	 */
	String getImplementation();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleDescriptorType#getImplementation <em>Implementation</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Implementation</em>' attribute.
	 * @see #getImplementation()
	 * @generated
	 */
	void setImplementation(String value);

	/**
	 * Returns the value of the '<em><b>Inbound Endpoint</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Inbound Endpoint</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Inbound Endpoint</em>' attribute.
	 * @see #setInboundEndpoint(String)
	 * @see org.mule.schema.MulePackage#getMuleDescriptorType_InboundEndpoint()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='inboundEndpoint' namespace='##targetNamespace'"
	 * @generated
	 */
	String getInboundEndpoint();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleDescriptorType#getInboundEndpoint <em>Inbound Endpoint</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Inbound Endpoint</em>' attribute.
	 * @see #getInboundEndpoint()
	 * @generated
	 */
	void setInboundEndpoint(String value);

	/**
	 * Returns the value of the '<em><b>Inbound Transformer</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Inbound Transformer</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Inbound Transformer</em>' attribute.
	 * @see #setInboundTransformer(String)
	 * @see org.mule.schema.MulePackage#getMuleDescriptorType_InboundTransformer()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='inboundTransformer' namespace='##targetNamespace'"
	 * @generated
	 */
	String getInboundTransformer();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleDescriptorType#getInboundTransformer <em>Inbound Transformer</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Inbound Transformer</em>' attribute.
	 * @see #getInboundTransformer()
	 * @generated
	 */
	void setInboundTransformer(String value);

	/**
	 * Returns the value of the '<em><b>Initial State</b></em>' attribute.
	 * The default value is <code>"started"</code>.
	 * The literals are from the enumeration {@link org.mule.schema.InitialStateType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Initial State</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Initial State</em>' attribute.
	 * @see org.mule.schema.InitialStateType
	 * @see #isSetInitialState()
	 * @see #unsetInitialState()
	 * @see #setInitialState(InitialStateType)
	 * @see org.mule.schema.MulePackage#getMuleDescriptorType_InitialState()
	 * @model default="started" unique="false" unsettable="true"
	 *        extendedMetaData="kind='attribute' name='initialState' namespace='##targetNamespace'"
	 * @generated
	 */
	InitialStateType getInitialState();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleDescriptorType#getInitialState <em>Initial State</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Initial State</em>' attribute.
	 * @see org.mule.schema.InitialStateType
	 * @see #isSetInitialState()
	 * @see #unsetInitialState()
	 * @see #getInitialState()
	 * @generated
	 */
	void setInitialState(InitialStateType value);

	/**
	 * Unsets the value of the '{@link org.mule.schema.MuleDescriptorType#getInitialState <em>Initial State</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetInitialState()
	 * @see #getInitialState()
	 * @see #setInitialState(InitialStateType)
	 * @generated
	 */
	void unsetInitialState();

	/**
	 * Returns whether the value of the '{@link org.mule.schema.MuleDescriptorType#getInitialState <em>Initial State</em>}' attribute is set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return whether the value of the '<em>Initial State</em>' attribute is set.
	 * @see #unsetInitialState()
	 * @see #getInitialState()
	 * @see #setInitialState(InitialStateType)
	 * @generated
	 */
	boolean isSetInitialState();

	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Name</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see org.mule.schema.MulePackage#getMuleDescriptorType_Name()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
	 *        extendedMetaData="kind='attribute' name='name' namespace='##targetNamespace'"
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleDescriptorType#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Returns the value of the '<em><b>Outbound Endpoint</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Outbound Endpoint</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Outbound Endpoint</em>' attribute.
	 * @see #setOutboundEndpoint(String)
	 * @see org.mule.schema.MulePackage#getMuleDescriptorType_OutboundEndpoint()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='outboundEndpoint' namespace='##targetNamespace'"
	 * @generated
	 */
	String getOutboundEndpoint();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleDescriptorType#getOutboundEndpoint <em>Outbound Endpoint</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Outbound Endpoint</em>' attribute.
	 * @see #getOutboundEndpoint()
	 * @generated
	 */
	void setOutboundEndpoint(String value);

	/**
	 * Returns the value of the '<em><b>Outbound Transformer</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Outbound Transformer</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Outbound Transformer</em>' attribute.
	 * @see #setOutboundTransformer(String)
	 * @see org.mule.schema.MulePackage#getMuleDescriptorType_OutboundTransformer()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='outboundTransformer' namespace='##targetNamespace'"
	 * @generated
	 */
	String getOutboundTransformer();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleDescriptorType#getOutboundTransformer <em>Outbound Transformer</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Outbound Transformer</em>' attribute.
	 * @see #getOutboundTransformer()
	 * @generated
	 */
	void setOutboundTransformer(String value);

	/**
	 * Returns the value of the '<em><b>Ref</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Ref</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Ref</em>' attribute.
	 * @see #setRef(String)
	 * @see org.mule.schema.MulePackage#getMuleDescriptorType_Ref()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='ref' namespace='##targetNamespace'"
	 * @generated
	 */
	String getRef();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleDescriptorType#getRef <em>Ref</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Ref</em>' attribute.
	 * @see #getRef()
	 * @generated
	 */
	void setRef(String value);

	/**
	 * Returns the value of the '<em><b>Response Transformer</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Response Transformer</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Response Transformer</em>' attribute.
	 * @see #setResponseTransformer(String)
	 * @see org.mule.schema.MulePackage#getMuleDescriptorType_ResponseTransformer()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='responseTransformer' namespace='##targetNamespace'"
	 * @generated
	 */
	String getResponseTransformer();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleDescriptorType#getResponseTransformer <em>Response Transformer</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Response Transformer</em>' attribute.
	 * @see #getResponseTransformer()
	 * @generated
	 */
	void setResponseTransformer(String value);

	/**
	 * Returns the value of the '<em><b>Singleton</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Singleton</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Singleton</em>' attribute.
	 * @see #isSetSingleton()
	 * @see #unsetSingleton()
	 * @see #setSingleton(boolean)
	 * @see org.mule.schema.MulePackage#getMuleDescriptorType_Singleton()
	 * @model default="false" unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.Boolean"
	 *        extendedMetaData="kind='attribute' name='singleton' namespace='##targetNamespace'"
	 * @generated
	 */
	boolean isSingleton();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleDescriptorType#isSingleton <em>Singleton</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Singleton</em>' attribute.
	 * @see #isSetSingleton()
	 * @see #unsetSingleton()
	 * @see #isSingleton()
	 * @generated
	 */
	void setSingleton(boolean value);

	/**
	 * Unsets the value of the '{@link org.mule.schema.MuleDescriptorType#isSingleton <em>Singleton</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSetSingleton()
	 * @see #isSingleton()
	 * @see #setSingleton(boolean)
	 * @generated
	 */
	void unsetSingleton();

	/**
	 * Returns whether the value of the '{@link org.mule.schema.MuleDescriptorType#isSingleton <em>Singleton</em>}' attribute is set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return whether the value of the '<em>Singleton</em>' attribute is set.
	 * @see #unsetSingleton()
	 * @see #isSingleton()
	 * @see #setSingleton(boolean)
	 * @generated
	 */
	boolean isSetSingleton();

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
	 * @see org.mule.schema.MulePackage#getMuleDescriptorType_Version()
	 * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String"
	 *        extendedMetaData="kind='attribute' name='version' namespace='##targetNamespace'"
	 * @generated
	 */
	String getVersion();

	/**
	 * Sets the value of the '{@link org.mule.schema.MuleDescriptorType#getVersion <em>Version</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Version</em>' attribute.
	 * @see #getVersion()
	 * @generated
	 */
	void setVersion(String value);

} // MuleDescriptorType
