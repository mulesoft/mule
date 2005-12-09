/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.schema.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import org.eclipse.emf.ecore.util.BasicFeatureMap;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.ecore.util.InternalEList;

import org.mule.schema.ExceptionStrategyType;
import org.mule.schema.InboundRouterType;
import org.mule.schema.InitialStateType;
import org.mule.schema.MuleDescriptorType;
import org.mule.schema.MulePackage;
import org.mule.schema.OutboundRouterType;
import org.mule.schema.PoolingProfileType;
import org.mule.schema.PropertiesType;
import org.mule.schema.QueueProfileType;
import org.mule.schema.ResponseRouterType;
import org.mule.schema.ThreadingProfileType;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Descriptor Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.mule.schema.impl.MuleDescriptorTypeImpl#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleDescriptorTypeImpl#getInboundRouter <em>Inbound Router</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleDescriptorTypeImpl#getOutboundRouter <em>Outbound Router</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleDescriptorTypeImpl#getResponseRouter <em>Response Router</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleDescriptorTypeImpl#getInterceptor <em>Interceptor</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleDescriptorTypeImpl#getThreadingProfile <em>Threading Profile</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleDescriptorTypeImpl#getPoolingProfile <em>Pooling Profile</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleDescriptorTypeImpl#getQueueProfile <em>Queue Profile</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleDescriptorTypeImpl#getExceptionStrategy <em>Exception Strategy</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleDescriptorTypeImpl#getProperties <em>Properties</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleDescriptorTypeImpl#isContainerManaged <em>Container Managed</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleDescriptorTypeImpl#getImplementation <em>Implementation</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleDescriptorTypeImpl#getInboundEndpoint <em>Inbound Endpoint</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleDescriptorTypeImpl#getInboundTransformer <em>Inbound Transformer</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleDescriptorTypeImpl#getInitialState <em>Initial State</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleDescriptorTypeImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleDescriptorTypeImpl#getOutboundEndpoint <em>Outbound Endpoint</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleDescriptorTypeImpl#getOutboundTransformer <em>Outbound Transformer</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleDescriptorTypeImpl#getRef <em>Ref</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleDescriptorTypeImpl#getResponseTransformer <em>Response Transformer</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleDescriptorTypeImpl#isSingleton <em>Singleton</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleDescriptorTypeImpl#getVersion <em>Version</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class MuleDescriptorTypeImpl extends EObjectImpl implements MuleDescriptorType {
	/**
	 * The cached value of the '{@link #getMixed() <em>Mixed</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMixed()
	 * @generated
	 * @ordered
	 */
	protected FeatureMap mixed = null;

	/**
	 * The default value of the '{@link #isContainerManaged() <em>Container Managed</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isContainerManaged()
	 * @generated
	 * @ordered
	 */
	protected static final boolean CONTAINER_MANAGED_EDEFAULT = true;

	/**
	 * The cached value of the '{@link #isContainerManaged() <em>Container Managed</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isContainerManaged()
	 * @generated
	 * @ordered
	 */
	protected boolean containerManaged = CONTAINER_MANAGED_EDEFAULT;

	/**
	 * This is true if the Container Managed attribute has been set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	protected boolean containerManagedESet = false;

	/**
	 * The default value of the '{@link #getImplementation() <em>Implementation</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getImplementation()
	 * @generated
	 * @ordered
	 */
	protected static final String IMPLEMENTATION_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getImplementation() <em>Implementation</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getImplementation()
	 * @generated
	 * @ordered
	 */
	protected String implementation = IMPLEMENTATION_EDEFAULT;

	/**
	 * The default value of the '{@link #getInboundEndpoint() <em>Inbound Endpoint</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getInboundEndpoint()
	 * @generated
	 * @ordered
	 */
	protected static final String INBOUND_ENDPOINT_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getInboundEndpoint() <em>Inbound Endpoint</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getInboundEndpoint()
	 * @generated
	 * @ordered
	 */
	protected String inboundEndpoint = INBOUND_ENDPOINT_EDEFAULT;

	/**
	 * The default value of the '{@link #getInboundTransformer() <em>Inbound Transformer</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getInboundTransformer()
	 * @generated
	 * @ordered
	 */
	protected static final String INBOUND_TRANSFORMER_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getInboundTransformer() <em>Inbound Transformer</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getInboundTransformer()
	 * @generated
	 * @ordered
	 */
	protected String inboundTransformer = INBOUND_TRANSFORMER_EDEFAULT;

	/**
	 * The default value of the '{@link #getInitialState() <em>Initial State</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getInitialState()
	 * @generated
	 * @ordered
	 */
	protected static final InitialStateType INITIAL_STATE_EDEFAULT = InitialStateType.STARTED_LITERAL;

	/**
	 * The cached value of the '{@link #getInitialState() <em>Initial State</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getInitialState()
	 * @generated
	 * @ordered
	 */
	protected InitialStateType initialState = INITIAL_STATE_EDEFAULT;

	/**
	 * This is true if the Initial State attribute has been set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	protected boolean initialStateESet = false;

	/**
	 * The default value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected static final String NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected String name = NAME_EDEFAULT;

	/**
	 * The default value of the '{@link #getOutboundEndpoint() <em>Outbound Endpoint</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOutboundEndpoint()
	 * @generated
	 * @ordered
	 */
	protected static final String OUTBOUND_ENDPOINT_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getOutboundEndpoint() <em>Outbound Endpoint</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOutboundEndpoint()
	 * @generated
	 * @ordered
	 */
	protected String outboundEndpoint = OUTBOUND_ENDPOINT_EDEFAULT;

	/**
	 * The default value of the '{@link #getOutboundTransformer() <em>Outbound Transformer</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOutboundTransformer()
	 * @generated
	 * @ordered
	 */
	protected static final String OUTBOUND_TRANSFORMER_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getOutboundTransformer() <em>Outbound Transformer</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOutboundTransformer()
	 * @generated
	 * @ordered
	 */
	protected String outboundTransformer = OUTBOUND_TRANSFORMER_EDEFAULT;

	/**
	 * The default value of the '{@link #getRef() <em>Ref</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRef()
	 * @generated
	 * @ordered
	 */
	protected static final String REF_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getRef() <em>Ref</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRef()
	 * @generated
	 * @ordered
	 */
	protected String ref = REF_EDEFAULT;

	/**
	 * The default value of the '{@link #getResponseTransformer() <em>Response Transformer</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getResponseTransformer()
	 * @generated
	 * @ordered
	 */
	protected static final String RESPONSE_TRANSFORMER_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getResponseTransformer() <em>Response Transformer</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getResponseTransformer()
	 * @generated
	 * @ordered
	 */
	protected String responseTransformer = RESPONSE_TRANSFORMER_EDEFAULT;

	/**
	 * The default value of the '{@link #isSingleton() <em>Singleton</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSingleton()
	 * @generated
	 * @ordered
	 */
	protected static final boolean SINGLETON_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isSingleton() <em>Singleton</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSingleton()
	 * @generated
	 * @ordered
	 */
	protected boolean singleton = SINGLETON_EDEFAULT;

	/**
	 * This is true if the Singleton attribute has been set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	protected boolean singletonESet = false;

	/**
	 * The default value of the '{@link #getVersion() <em>Version</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getVersion()
	 * @generated
	 * @ordered
	 */
	protected static final String VERSION_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getVersion() <em>Version</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getVersion()
	 * @generated
	 * @ordered
	 */
	protected String version = VERSION_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected MuleDescriptorTypeImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EClass eStaticClass() {
		return MulePackage.eINSTANCE.getMuleDescriptorType();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FeatureMap getMixed() {
		if (mixed == null) {
			mixed = new BasicFeatureMap(this, MulePackage.MULE_DESCRIPTOR_TYPE__MIXED);
		}
		return mixed;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public InboundRouterType getInboundRouter() {
		return (InboundRouterType)getMixed().get(MulePackage.eINSTANCE.getMuleDescriptorType_InboundRouter(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetInboundRouter(InboundRouterType newInboundRouter, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getMuleDescriptorType_InboundRouter(), newInboundRouter, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setInboundRouter(InboundRouterType newInboundRouter) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getMuleDescriptorType_InboundRouter(), newInboundRouter);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public OutboundRouterType getOutboundRouter() {
		return (OutboundRouterType)getMixed().get(MulePackage.eINSTANCE.getMuleDescriptorType_OutboundRouter(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetOutboundRouter(OutboundRouterType newOutboundRouter, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getMuleDescriptorType_OutboundRouter(), newOutboundRouter, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setOutboundRouter(OutboundRouterType newOutboundRouter) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getMuleDescriptorType_OutboundRouter(), newOutboundRouter);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ResponseRouterType getResponseRouter() {
		return (ResponseRouterType)getMixed().get(MulePackage.eINSTANCE.getMuleDescriptorType_ResponseRouter(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetResponseRouter(ResponseRouterType newResponseRouter, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getMuleDescriptorType_ResponseRouter(), newResponseRouter, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setResponseRouter(ResponseRouterType newResponseRouter) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getMuleDescriptorType_ResponseRouter(), newResponseRouter);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getInterceptor() {
		return ((FeatureMap)getMixed()).list(MulePackage.eINSTANCE.getMuleDescriptorType_Interceptor());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ThreadingProfileType getThreadingProfile() {
		return (ThreadingProfileType)getMixed().get(MulePackage.eINSTANCE.getMuleDescriptorType_ThreadingProfile(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetThreadingProfile(ThreadingProfileType newThreadingProfile, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getMuleDescriptorType_ThreadingProfile(), newThreadingProfile, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setThreadingProfile(ThreadingProfileType newThreadingProfile) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getMuleDescriptorType_ThreadingProfile(), newThreadingProfile);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PoolingProfileType getPoolingProfile() {
		return (PoolingProfileType)getMixed().get(MulePackage.eINSTANCE.getMuleDescriptorType_PoolingProfile(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetPoolingProfile(PoolingProfileType newPoolingProfile, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getMuleDescriptorType_PoolingProfile(), newPoolingProfile, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setPoolingProfile(PoolingProfileType newPoolingProfile) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getMuleDescriptorType_PoolingProfile(), newPoolingProfile);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public QueueProfileType getQueueProfile() {
		return (QueueProfileType)getMixed().get(MulePackage.eINSTANCE.getMuleDescriptorType_QueueProfile(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetQueueProfile(QueueProfileType newQueueProfile, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getMuleDescriptorType_QueueProfile(), newQueueProfile, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setQueueProfile(QueueProfileType newQueueProfile) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getMuleDescriptorType_QueueProfile(), newQueueProfile);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ExceptionStrategyType getExceptionStrategy() {
		return (ExceptionStrategyType)getMixed().get(MulePackage.eINSTANCE.getMuleDescriptorType_ExceptionStrategy(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetExceptionStrategy(ExceptionStrategyType newExceptionStrategy, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getMuleDescriptorType_ExceptionStrategy(), newExceptionStrategy, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setExceptionStrategy(ExceptionStrategyType newExceptionStrategy) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getMuleDescriptorType_ExceptionStrategy(), newExceptionStrategy);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PropertiesType getProperties() {
		return (PropertiesType)getMixed().get(MulePackage.eINSTANCE.getMuleDescriptorType_Properties(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetProperties(PropertiesType newProperties, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getMuleDescriptorType_Properties(), newProperties, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setProperties(PropertiesType newProperties) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getMuleDescriptorType_Properties(), newProperties);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isContainerManaged() {
		return containerManaged;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setContainerManaged(boolean newContainerManaged) {
		boolean oldContainerManaged = containerManaged;
		containerManaged = newContainerManaged;
		boolean oldContainerManagedESet = containerManagedESet;
		containerManagedESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MULE_DESCRIPTOR_TYPE__CONTAINER_MANAGED, oldContainerManaged, containerManaged, !oldContainerManagedESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void unsetContainerManaged() {
		boolean oldContainerManaged = containerManaged;
		boolean oldContainerManagedESet = containerManagedESet;
		containerManaged = CONTAINER_MANAGED_EDEFAULT;
		containerManagedESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.MULE_DESCRIPTOR_TYPE__CONTAINER_MANAGED, oldContainerManaged, CONTAINER_MANAGED_EDEFAULT, oldContainerManagedESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSetContainerManaged() {
		return containerManagedESet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getImplementation() {
		return implementation;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setImplementation(String newImplementation) {
		String oldImplementation = implementation;
		implementation = newImplementation;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MULE_DESCRIPTOR_TYPE__IMPLEMENTATION, oldImplementation, implementation));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getInboundEndpoint() {
		return inboundEndpoint;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setInboundEndpoint(String newInboundEndpoint) {
		String oldInboundEndpoint = inboundEndpoint;
		inboundEndpoint = newInboundEndpoint;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MULE_DESCRIPTOR_TYPE__INBOUND_ENDPOINT, oldInboundEndpoint, inboundEndpoint));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getInboundTransformer() {
		return inboundTransformer;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setInboundTransformer(String newInboundTransformer) {
		String oldInboundTransformer = inboundTransformer;
		inboundTransformer = newInboundTransformer;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MULE_DESCRIPTOR_TYPE__INBOUND_TRANSFORMER, oldInboundTransformer, inboundTransformer));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public InitialStateType getInitialState() {
		return initialState;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setInitialState(InitialStateType newInitialState) {
		InitialStateType oldInitialState = initialState;
		initialState = newInitialState == null ? INITIAL_STATE_EDEFAULT : newInitialState;
		boolean oldInitialStateESet = initialStateESet;
		initialStateESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MULE_DESCRIPTOR_TYPE__INITIAL_STATE, oldInitialState, initialState, !oldInitialStateESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void unsetInitialState() {
		InitialStateType oldInitialState = initialState;
		boolean oldInitialStateESet = initialStateESet;
		initialState = INITIAL_STATE_EDEFAULT;
		initialStateESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.MULE_DESCRIPTOR_TYPE__INITIAL_STATE, oldInitialState, INITIAL_STATE_EDEFAULT, oldInitialStateESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSetInitialState() {
		return initialStateESet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getName() {
		return name;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setName(String newName) {
		String oldName = name;
		name = newName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MULE_DESCRIPTOR_TYPE__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getOutboundEndpoint() {
		return outboundEndpoint;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setOutboundEndpoint(String newOutboundEndpoint) {
		String oldOutboundEndpoint = outboundEndpoint;
		outboundEndpoint = newOutboundEndpoint;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MULE_DESCRIPTOR_TYPE__OUTBOUND_ENDPOINT, oldOutboundEndpoint, outboundEndpoint));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getOutboundTransformer() {
		return outboundTransformer;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setOutboundTransformer(String newOutboundTransformer) {
		String oldOutboundTransformer = outboundTransformer;
		outboundTransformer = newOutboundTransformer;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MULE_DESCRIPTOR_TYPE__OUTBOUND_TRANSFORMER, oldOutboundTransformer, outboundTransformer));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getRef() {
		return ref;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setRef(String newRef) {
		String oldRef = ref;
		ref = newRef;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MULE_DESCRIPTOR_TYPE__REF, oldRef, ref));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getResponseTransformer() {
		return responseTransformer;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setResponseTransformer(String newResponseTransformer) {
		String oldResponseTransformer = responseTransformer;
		responseTransformer = newResponseTransformer;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MULE_DESCRIPTOR_TYPE__RESPONSE_TRANSFORMER, oldResponseTransformer, responseTransformer));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSingleton() {
		return singleton;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setSingleton(boolean newSingleton) {
		boolean oldSingleton = singleton;
		singleton = newSingleton;
		boolean oldSingletonESet = singletonESet;
		singletonESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MULE_DESCRIPTOR_TYPE__SINGLETON, oldSingleton, singleton, !oldSingletonESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void unsetSingleton() {
		boolean oldSingleton = singleton;
		boolean oldSingletonESet = singletonESet;
		singleton = SINGLETON_EDEFAULT;
		singletonESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.MULE_DESCRIPTOR_TYPE__SINGLETON, oldSingleton, SINGLETON_EDEFAULT, oldSingletonESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSetSingleton() {
		return singletonESet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setVersion(String newVersion) {
		String oldVersion = version;
		version = newVersion;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MULE_DESCRIPTOR_TYPE__VERSION, oldVersion, version));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, Class baseClass, NotificationChain msgs) {
		if (featureID >= 0) {
			switch (eDerivedStructuralFeatureID(featureID, baseClass)) {
				case MulePackage.MULE_DESCRIPTOR_TYPE__MIXED:
					return ((InternalEList)getMixed()).basicRemove(otherEnd, msgs);
				case MulePackage.MULE_DESCRIPTOR_TYPE__INBOUND_ROUTER:
					return basicSetInboundRouter(null, msgs);
				case MulePackage.MULE_DESCRIPTOR_TYPE__OUTBOUND_ROUTER:
					return basicSetOutboundRouter(null, msgs);
				case MulePackage.MULE_DESCRIPTOR_TYPE__RESPONSE_ROUTER:
					return basicSetResponseRouter(null, msgs);
				case MulePackage.MULE_DESCRIPTOR_TYPE__INTERCEPTOR:
					return ((InternalEList)getInterceptor()).basicRemove(otherEnd, msgs);
				case MulePackage.MULE_DESCRIPTOR_TYPE__THREADING_PROFILE:
					return basicSetThreadingProfile(null, msgs);
				case MulePackage.MULE_DESCRIPTOR_TYPE__POOLING_PROFILE:
					return basicSetPoolingProfile(null, msgs);
				case MulePackage.MULE_DESCRIPTOR_TYPE__QUEUE_PROFILE:
					return basicSetQueueProfile(null, msgs);
				case MulePackage.MULE_DESCRIPTOR_TYPE__EXCEPTION_STRATEGY:
					return basicSetExceptionStrategy(null, msgs);
				case MulePackage.MULE_DESCRIPTOR_TYPE__PROPERTIES:
					return basicSetProperties(null, msgs);
				default:
					return eDynamicInverseRemove(otherEnd, featureID, baseClass, msgs);
			}
		}
		return eBasicSetContainer(null, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Object eGet(EStructuralFeature eFeature, boolean resolve) {
		switch (eDerivedStructuralFeatureID(eFeature)) {
			case MulePackage.MULE_DESCRIPTOR_TYPE__MIXED:
				return getMixed();
			case MulePackage.MULE_DESCRIPTOR_TYPE__INBOUND_ROUTER:
				return getInboundRouter();
			case MulePackage.MULE_DESCRIPTOR_TYPE__OUTBOUND_ROUTER:
				return getOutboundRouter();
			case MulePackage.MULE_DESCRIPTOR_TYPE__RESPONSE_ROUTER:
				return getResponseRouter();
			case MulePackage.MULE_DESCRIPTOR_TYPE__INTERCEPTOR:
				return getInterceptor();
			case MulePackage.MULE_DESCRIPTOR_TYPE__THREADING_PROFILE:
				return getThreadingProfile();
			case MulePackage.MULE_DESCRIPTOR_TYPE__POOLING_PROFILE:
				return getPoolingProfile();
			case MulePackage.MULE_DESCRIPTOR_TYPE__QUEUE_PROFILE:
				return getQueueProfile();
			case MulePackage.MULE_DESCRIPTOR_TYPE__EXCEPTION_STRATEGY:
				return getExceptionStrategy();
			case MulePackage.MULE_DESCRIPTOR_TYPE__PROPERTIES:
				return getProperties();
			case MulePackage.MULE_DESCRIPTOR_TYPE__CONTAINER_MANAGED:
				return isContainerManaged() ? Boolean.TRUE : Boolean.FALSE;
			case MulePackage.MULE_DESCRIPTOR_TYPE__IMPLEMENTATION:
				return getImplementation();
			case MulePackage.MULE_DESCRIPTOR_TYPE__INBOUND_ENDPOINT:
				return getInboundEndpoint();
			case MulePackage.MULE_DESCRIPTOR_TYPE__INBOUND_TRANSFORMER:
				return getInboundTransformer();
			case MulePackage.MULE_DESCRIPTOR_TYPE__INITIAL_STATE:
				return getInitialState();
			case MulePackage.MULE_DESCRIPTOR_TYPE__NAME:
				return getName();
			case MulePackage.MULE_DESCRIPTOR_TYPE__OUTBOUND_ENDPOINT:
				return getOutboundEndpoint();
			case MulePackage.MULE_DESCRIPTOR_TYPE__OUTBOUND_TRANSFORMER:
				return getOutboundTransformer();
			case MulePackage.MULE_DESCRIPTOR_TYPE__REF:
				return getRef();
			case MulePackage.MULE_DESCRIPTOR_TYPE__RESPONSE_TRANSFORMER:
				return getResponseTransformer();
			case MulePackage.MULE_DESCRIPTOR_TYPE__SINGLETON:
				return isSingleton() ? Boolean.TRUE : Boolean.FALSE;
			case MulePackage.MULE_DESCRIPTOR_TYPE__VERSION:
				return getVersion();
		}
		return eDynamicGet(eFeature, resolve);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void eSet(EStructuralFeature eFeature, Object newValue) {
		switch (eDerivedStructuralFeatureID(eFeature)) {
			case MulePackage.MULE_DESCRIPTOR_TYPE__MIXED:
				getMixed().clear();
				getMixed().addAll((Collection)newValue);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__INBOUND_ROUTER:
				setInboundRouter((InboundRouterType)newValue);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__OUTBOUND_ROUTER:
				setOutboundRouter((OutboundRouterType)newValue);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__RESPONSE_ROUTER:
				setResponseRouter((ResponseRouterType)newValue);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__INTERCEPTOR:
				getInterceptor().clear();
				getInterceptor().addAll((Collection)newValue);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__THREADING_PROFILE:
				setThreadingProfile((ThreadingProfileType)newValue);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__POOLING_PROFILE:
				setPoolingProfile((PoolingProfileType)newValue);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__QUEUE_PROFILE:
				setQueueProfile((QueueProfileType)newValue);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__EXCEPTION_STRATEGY:
				setExceptionStrategy((ExceptionStrategyType)newValue);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__PROPERTIES:
				setProperties((PropertiesType)newValue);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__CONTAINER_MANAGED:
				setContainerManaged(((Boolean)newValue).booleanValue());
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__IMPLEMENTATION:
				setImplementation((String)newValue);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__INBOUND_ENDPOINT:
				setInboundEndpoint((String)newValue);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__INBOUND_TRANSFORMER:
				setInboundTransformer((String)newValue);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__INITIAL_STATE:
				setInitialState((InitialStateType)newValue);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__NAME:
				setName((String)newValue);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__OUTBOUND_ENDPOINT:
				setOutboundEndpoint((String)newValue);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__OUTBOUND_TRANSFORMER:
				setOutboundTransformer((String)newValue);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__REF:
				setRef((String)newValue);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__RESPONSE_TRANSFORMER:
				setResponseTransformer((String)newValue);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__SINGLETON:
				setSingleton(((Boolean)newValue).booleanValue());
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__VERSION:
				setVersion((String)newValue);
				return;
		}
		eDynamicSet(eFeature, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void eUnset(EStructuralFeature eFeature) {
		switch (eDerivedStructuralFeatureID(eFeature)) {
			case MulePackage.MULE_DESCRIPTOR_TYPE__MIXED:
				getMixed().clear();
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__INBOUND_ROUTER:
				setInboundRouter((InboundRouterType)null);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__OUTBOUND_ROUTER:
				setOutboundRouter((OutboundRouterType)null);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__RESPONSE_ROUTER:
				setResponseRouter((ResponseRouterType)null);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__INTERCEPTOR:
				getInterceptor().clear();
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__THREADING_PROFILE:
				setThreadingProfile((ThreadingProfileType)null);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__POOLING_PROFILE:
				setPoolingProfile((PoolingProfileType)null);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__QUEUE_PROFILE:
				setQueueProfile((QueueProfileType)null);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__EXCEPTION_STRATEGY:
				setExceptionStrategy((ExceptionStrategyType)null);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__PROPERTIES:
				setProperties((PropertiesType)null);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__CONTAINER_MANAGED:
				unsetContainerManaged();
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__IMPLEMENTATION:
				setImplementation(IMPLEMENTATION_EDEFAULT);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__INBOUND_ENDPOINT:
				setInboundEndpoint(INBOUND_ENDPOINT_EDEFAULT);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__INBOUND_TRANSFORMER:
				setInboundTransformer(INBOUND_TRANSFORMER_EDEFAULT);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__INITIAL_STATE:
				unsetInitialState();
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__NAME:
				setName(NAME_EDEFAULT);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__OUTBOUND_ENDPOINT:
				setOutboundEndpoint(OUTBOUND_ENDPOINT_EDEFAULT);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__OUTBOUND_TRANSFORMER:
				setOutboundTransformer(OUTBOUND_TRANSFORMER_EDEFAULT);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__REF:
				setRef(REF_EDEFAULT);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__RESPONSE_TRANSFORMER:
				setResponseTransformer(RESPONSE_TRANSFORMER_EDEFAULT);
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__SINGLETON:
				unsetSingleton();
				return;
			case MulePackage.MULE_DESCRIPTOR_TYPE__VERSION:
				setVersion(VERSION_EDEFAULT);
				return;
		}
		eDynamicUnset(eFeature);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean eIsSet(EStructuralFeature eFeature) {
		switch (eDerivedStructuralFeatureID(eFeature)) {
			case MulePackage.MULE_DESCRIPTOR_TYPE__MIXED:
				return mixed != null && !mixed.isEmpty();
			case MulePackage.MULE_DESCRIPTOR_TYPE__INBOUND_ROUTER:
				return getInboundRouter() != null;
			case MulePackage.MULE_DESCRIPTOR_TYPE__OUTBOUND_ROUTER:
				return getOutboundRouter() != null;
			case MulePackage.MULE_DESCRIPTOR_TYPE__RESPONSE_ROUTER:
				return getResponseRouter() != null;
			case MulePackage.MULE_DESCRIPTOR_TYPE__INTERCEPTOR:
				return !getInterceptor().isEmpty();
			case MulePackage.MULE_DESCRIPTOR_TYPE__THREADING_PROFILE:
				return getThreadingProfile() != null;
			case MulePackage.MULE_DESCRIPTOR_TYPE__POOLING_PROFILE:
				return getPoolingProfile() != null;
			case MulePackage.MULE_DESCRIPTOR_TYPE__QUEUE_PROFILE:
				return getQueueProfile() != null;
			case MulePackage.MULE_DESCRIPTOR_TYPE__EXCEPTION_STRATEGY:
				return getExceptionStrategy() != null;
			case MulePackage.MULE_DESCRIPTOR_TYPE__PROPERTIES:
				return getProperties() != null;
			case MulePackage.MULE_DESCRIPTOR_TYPE__CONTAINER_MANAGED:
				return isSetContainerManaged();
			case MulePackage.MULE_DESCRIPTOR_TYPE__IMPLEMENTATION:
				return IMPLEMENTATION_EDEFAULT == null ? implementation != null : !IMPLEMENTATION_EDEFAULT.equals(implementation);
			case MulePackage.MULE_DESCRIPTOR_TYPE__INBOUND_ENDPOINT:
				return INBOUND_ENDPOINT_EDEFAULT == null ? inboundEndpoint != null : !INBOUND_ENDPOINT_EDEFAULT.equals(inboundEndpoint);
			case MulePackage.MULE_DESCRIPTOR_TYPE__INBOUND_TRANSFORMER:
				return INBOUND_TRANSFORMER_EDEFAULT == null ? inboundTransformer != null : !INBOUND_TRANSFORMER_EDEFAULT.equals(inboundTransformer);
			case MulePackage.MULE_DESCRIPTOR_TYPE__INITIAL_STATE:
				return isSetInitialState();
			case MulePackage.MULE_DESCRIPTOR_TYPE__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case MulePackage.MULE_DESCRIPTOR_TYPE__OUTBOUND_ENDPOINT:
				return OUTBOUND_ENDPOINT_EDEFAULT == null ? outboundEndpoint != null : !OUTBOUND_ENDPOINT_EDEFAULT.equals(outboundEndpoint);
			case MulePackage.MULE_DESCRIPTOR_TYPE__OUTBOUND_TRANSFORMER:
				return OUTBOUND_TRANSFORMER_EDEFAULT == null ? outboundTransformer != null : !OUTBOUND_TRANSFORMER_EDEFAULT.equals(outboundTransformer);
			case MulePackage.MULE_DESCRIPTOR_TYPE__REF:
				return REF_EDEFAULT == null ? ref != null : !REF_EDEFAULT.equals(ref);
			case MulePackage.MULE_DESCRIPTOR_TYPE__RESPONSE_TRANSFORMER:
				return RESPONSE_TRANSFORMER_EDEFAULT == null ? responseTransformer != null : !RESPONSE_TRANSFORMER_EDEFAULT.equals(responseTransformer);
			case MulePackage.MULE_DESCRIPTOR_TYPE__SINGLETON:
				return isSetSingleton();
			case MulePackage.MULE_DESCRIPTOR_TYPE__VERSION:
				return VERSION_EDEFAULT == null ? version != null : !VERSION_EDEFAULT.equals(version);
		}
		return eDynamicIsSet(eFeature);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (mixed: ");
		result.append(mixed);
		result.append(", containerManaged: ");
		if (containerManagedESet) result.append(containerManaged); else result.append("<unset>");
		result.append(", implementation: ");
		result.append(implementation);
		result.append(", inboundEndpoint: ");
		result.append(inboundEndpoint);
		result.append(", inboundTransformer: ");
		result.append(inboundTransformer);
		result.append(", initialState: ");
		if (initialStateESet) result.append(initialState); else result.append("<unset>");
		result.append(", name: ");
		result.append(name);
		result.append(", outboundEndpoint: ");
		result.append(outboundEndpoint);
		result.append(", outboundTransformer: ");
		result.append(outboundTransformer);
		result.append(", ref: ");
		result.append(ref);
		result.append(", responseTransformer: ");
		result.append(responseTransformer);
		result.append(", singleton: ");
		if (singletonESet) result.append(singleton); else result.append("<unset>");
		result.append(", version: ");
		result.append(version);
		result.append(')');
		return result.toString();
	}

} //MuleDescriptorTypeImpl
