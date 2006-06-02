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

import org.mule.schema.AgentsType;
import org.mule.schema.EndpointIdentifiersType;
import org.mule.schema.EnvironmentPropertiesType;
import org.mule.schema.GlobalEndpointsType;
import org.mule.schema.ModelType;
import org.mule.schema.MuleConfigurationType;
import org.mule.schema.MuleEnvironmentPropertiesType;
import org.mule.schema.MulePackage;
import org.mule.schema.SecurityManagerType;
import org.mule.schema.TransactionManagerType;
import org.mule.schema.TransformersType;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Configuration Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.mule.schema.impl.MuleConfigurationTypeImpl#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleConfigurationTypeImpl#getDescription <em>Description</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleConfigurationTypeImpl#getEnvironmentProperties <em>Environment Properties</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleConfigurationTypeImpl#getMuleEnvironmentProperties <em>Mule Environment Properties</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleConfigurationTypeImpl#getContainerContext <em>Container Context</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleConfigurationTypeImpl#getSecurityManager <em>Security Manager</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleConfigurationTypeImpl#getTransactionManager <em>Transaction Manager</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleConfigurationTypeImpl#getAgents <em>Agents</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleConfigurationTypeImpl#getConnector <em>Connector</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleConfigurationTypeImpl#getEndpointIdentifiers <em>Endpoint Identifiers</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleConfigurationTypeImpl#getTransformers <em>Transformers</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleConfigurationTypeImpl#getGlobalEndpoints <em>Global Endpoints</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleConfigurationTypeImpl#getInterceptorStack <em>Interceptor Stack</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleConfigurationTypeImpl#getModel <em>Model</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleConfigurationTypeImpl#getMuleDescriptor <em>Mule Descriptor</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleConfigurationTypeImpl#getId <em>Id</em>}</li>
 *   <li>{@link org.mule.schema.impl.MuleConfigurationTypeImpl#getVersion <em>Version</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class MuleConfigurationTypeImpl extends EObjectImpl implements MuleConfigurationType {
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
	 * The default value of the '{@link #getDescription() <em>Description</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDescription()
	 * @generated
	 * @ordered
	 */
	protected static final String DESCRIPTION_EDEFAULT = null;

	/**
	 * The default value of the '{@link #getId() <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getId()
	 * @generated
	 * @ordered
	 */
	protected static final String ID_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getId() <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getId()
	 * @generated
	 * @ordered
	 */
	protected String id = ID_EDEFAULT;

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
	protected MuleConfigurationTypeImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EClass eStaticClass() {
		return MulePackage.eINSTANCE.getMuleConfigurationType();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FeatureMap getMixed() {
		if (mixed == null) {
			mixed = new BasicFeatureMap(this, MulePackage.MULE_CONFIGURATION_TYPE__MIXED);
		}
		return mixed;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getDescription() {
		return (String)getMixed().get(MulePackage.eINSTANCE.getMuleConfigurationType_Description(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setDescription(String newDescription) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getMuleConfigurationType_Description(), newDescription);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EnvironmentPropertiesType getEnvironmentProperties() {
		return (EnvironmentPropertiesType)getMixed().get(MulePackage.eINSTANCE.getMuleConfigurationType_EnvironmentProperties(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetEnvironmentProperties(EnvironmentPropertiesType newEnvironmentProperties, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getMuleConfigurationType_EnvironmentProperties(), newEnvironmentProperties, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setEnvironmentProperties(EnvironmentPropertiesType newEnvironmentProperties) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getMuleConfigurationType_EnvironmentProperties(), newEnvironmentProperties);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public MuleEnvironmentPropertiesType getMuleEnvironmentProperties() {
		return (MuleEnvironmentPropertiesType)getMixed().get(MulePackage.eINSTANCE.getMuleConfigurationType_MuleEnvironmentProperties(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetMuleEnvironmentProperties(MuleEnvironmentPropertiesType newMuleEnvironmentProperties, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getMuleConfigurationType_MuleEnvironmentProperties(), newMuleEnvironmentProperties, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setMuleEnvironmentProperties(MuleEnvironmentPropertiesType newMuleEnvironmentProperties) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getMuleConfigurationType_MuleEnvironmentProperties(), newMuleEnvironmentProperties);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getContainerContext() {
		return ((FeatureMap)getMixed()).list(MulePackage.eINSTANCE.getMuleConfigurationType_ContainerContext());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public SecurityManagerType getSecurityManager() {
		return (SecurityManagerType)getMixed().get(MulePackage.eINSTANCE.getMuleConfigurationType_SecurityManager(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetSecurityManager(SecurityManagerType newSecurityManager, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getMuleConfigurationType_SecurityManager(), newSecurityManager, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setSecurityManager(SecurityManagerType newSecurityManager) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getMuleConfigurationType_SecurityManager(), newSecurityManager);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public TransactionManagerType getTransactionManager() {
		return (TransactionManagerType)getMixed().get(MulePackage.eINSTANCE.getMuleConfigurationType_TransactionManager(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetTransactionManager(TransactionManagerType newTransactionManager, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getMuleConfigurationType_TransactionManager(), newTransactionManager, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setTransactionManager(TransactionManagerType newTransactionManager) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getMuleConfigurationType_TransactionManager(), newTransactionManager);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public AgentsType getAgents() {
		return (AgentsType)getMixed().get(MulePackage.eINSTANCE.getMuleConfigurationType_Agents(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetAgents(AgentsType newAgents, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getMuleConfigurationType_Agents(), newAgents, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setAgents(AgentsType newAgents) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getMuleConfigurationType_Agents(), newAgents);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getConnector() {
		return ((FeatureMap)getMixed()).list(MulePackage.eINSTANCE.getMuleConfigurationType_Connector());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EndpointIdentifiersType getEndpointIdentifiers() {
		return (EndpointIdentifiersType)getMixed().get(MulePackage.eINSTANCE.getMuleConfigurationType_EndpointIdentifiers(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetEndpointIdentifiers(EndpointIdentifiersType newEndpointIdentifiers, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getMuleConfigurationType_EndpointIdentifiers(), newEndpointIdentifiers, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setEndpointIdentifiers(EndpointIdentifiersType newEndpointIdentifiers) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getMuleConfigurationType_EndpointIdentifiers(), newEndpointIdentifiers);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public TransformersType getTransformers() {
		return (TransformersType)getMixed().get(MulePackage.eINSTANCE.getMuleConfigurationType_Transformers(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetTransformers(TransformersType newTransformers, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getMuleConfigurationType_Transformers(), newTransformers, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setTransformers(TransformersType newTransformers) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getMuleConfigurationType_Transformers(), newTransformers);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public GlobalEndpointsType getGlobalEndpoints() {
		return (GlobalEndpointsType)getMixed().get(MulePackage.eINSTANCE.getMuleConfigurationType_GlobalEndpoints(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetGlobalEndpoints(GlobalEndpointsType newGlobalEndpoints, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getMuleConfigurationType_GlobalEndpoints(), newGlobalEndpoints, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setGlobalEndpoints(GlobalEndpointsType newGlobalEndpoints) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getMuleConfigurationType_GlobalEndpoints(), newGlobalEndpoints);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getInterceptorStack() {
		return ((FeatureMap)getMixed()).list(MulePackage.eINSTANCE.getMuleConfigurationType_InterceptorStack());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ModelType getModel() {
		return (ModelType)getMixed().get(MulePackage.eINSTANCE.getMuleConfigurationType_Model(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetModel(ModelType newModel, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getMuleConfigurationType_Model(), newModel, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setModel(ModelType newModel) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getMuleConfigurationType_Model(), newModel);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getMuleDescriptor() {
		return ((FeatureMap)getMixed()).list(MulePackage.eINSTANCE.getMuleConfigurationType_MuleDescriptor());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getId() {
		return id;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setId(String newId) {
		String oldId = id;
		id = newId;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MULE_CONFIGURATION_TYPE__ID, oldId, id));
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
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MULE_CONFIGURATION_TYPE__VERSION, oldVersion, version));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, Class baseClass, NotificationChain msgs) {
		if (featureID >= 0) {
			switch (eDerivedStructuralFeatureID(featureID, baseClass)) {
				case MulePackage.MULE_CONFIGURATION_TYPE__MIXED:
					return ((InternalEList)getMixed()).basicRemove(otherEnd, msgs);
				case MulePackage.MULE_CONFIGURATION_TYPE__ENVIRONMENT_PROPERTIES:
					return basicSetEnvironmentProperties(null, msgs);
				case MulePackage.MULE_CONFIGURATION_TYPE__MULE_ENVIRONMENT_PROPERTIES:
					return basicSetMuleEnvironmentProperties(null, msgs);
				case MulePackage.MULE_CONFIGURATION_TYPE__CONTAINER_CONTEXT:
					return ((InternalEList)getContainerContext()).basicRemove(otherEnd, msgs);
				case MulePackage.MULE_CONFIGURATION_TYPE__SECURITY_MANAGER:
					return basicSetSecurityManager(null, msgs);
				case MulePackage.MULE_CONFIGURATION_TYPE__TRANSACTION_MANAGER:
					return basicSetTransactionManager(null, msgs);
				case MulePackage.MULE_CONFIGURATION_TYPE__AGENTS:
					return basicSetAgents(null, msgs);
				case MulePackage.MULE_CONFIGURATION_TYPE__CONNECTOR:
					return ((InternalEList)getConnector()).basicRemove(otherEnd, msgs);
				case MulePackage.MULE_CONFIGURATION_TYPE__ENDPOINT_IDENTIFIERS:
					return basicSetEndpointIdentifiers(null, msgs);
				case MulePackage.MULE_CONFIGURATION_TYPE__TRANSFORMERS:
					return basicSetTransformers(null, msgs);
				case MulePackage.MULE_CONFIGURATION_TYPE__GLOBAL_ENDPOINTS:
					return basicSetGlobalEndpoints(null, msgs);
				case MulePackage.MULE_CONFIGURATION_TYPE__INTERCEPTOR_STACK:
					return ((InternalEList)getInterceptorStack()).basicRemove(otherEnd, msgs);
				case MulePackage.MULE_CONFIGURATION_TYPE__MODEL:
					return basicSetModel(null, msgs);
				case MulePackage.MULE_CONFIGURATION_TYPE__MULE_DESCRIPTOR:
					return ((InternalEList)getMuleDescriptor()).basicRemove(otherEnd, msgs);
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
			case MulePackage.MULE_CONFIGURATION_TYPE__MIXED:
				return getMixed();
			case MulePackage.MULE_CONFIGURATION_TYPE__DESCRIPTION:
				return getDescription();
			case MulePackage.MULE_CONFIGURATION_TYPE__ENVIRONMENT_PROPERTIES:
				return getEnvironmentProperties();
			case MulePackage.MULE_CONFIGURATION_TYPE__MULE_ENVIRONMENT_PROPERTIES:
				return getMuleEnvironmentProperties();
			case MulePackage.MULE_CONFIGURATION_TYPE__CONTAINER_CONTEXT:
				return getContainerContext();
			case MulePackage.MULE_CONFIGURATION_TYPE__SECURITY_MANAGER:
				return getSecurityManager();
			case MulePackage.MULE_CONFIGURATION_TYPE__TRANSACTION_MANAGER:
				return getTransactionManager();
			case MulePackage.MULE_CONFIGURATION_TYPE__AGENTS:
				return getAgents();
			case MulePackage.MULE_CONFIGURATION_TYPE__CONNECTOR:
				return getConnector();
			case MulePackage.MULE_CONFIGURATION_TYPE__ENDPOINT_IDENTIFIERS:
				return getEndpointIdentifiers();
			case MulePackage.MULE_CONFIGURATION_TYPE__TRANSFORMERS:
				return getTransformers();
			case MulePackage.MULE_CONFIGURATION_TYPE__GLOBAL_ENDPOINTS:
				return getGlobalEndpoints();
			case MulePackage.MULE_CONFIGURATION_TYPE__INTERCEPTOR_STACK:
				return getInterceptorStack();
			case MulePackage.MULE_CONFIGURATION_TYPE__MODEL:
				return getModel();
			case MulePackage.MULE_CONFIGURATION_TYPE__MULE_DESCRIPTOR:
				return getMuleDescriptor();
			case MulePackage.MULE_CONFIGURATION_TYPE__ID:
				return getId();
			case MulePackage.MULE_CONFIGURATION_TYPE__VERSION:
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
			case MulePackage.MULE_CONFIGURATION_TYPE__MIXED:
				getMixed().clear();
				getMixed().addAll((Collection)newValue);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__DESCRIPTION:
				setDescription((String)newValue);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__ENVIRONMENT_PROPERTIES:
				setEnvironmentProperties((EnvironmentPropertiesType)newValue);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__MULE_ENVIRONMENT_PROPERTIES:
				setMuleEnvironmentProperties((MuleEnvironmentPropertiesType)newValue);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__CONTAINER_CONTEXT:
				getContainerContext().clear();
				getContainerContext().addAll((Collection)newValue);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__SECURITY_MANAGER:
				setSecurityManager((SecurityManagerType)newValue);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__TRANSACTION_MANAGER:
				setTransactionManager((TransactionManagerType)newValue);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__AGENTS:
				setAgents((AgentsType)newValue);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__CONNECTOR:
				getConnector().clear();
				getConnector().addAll((Collection)newValue);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__ENDPOINT_IDENTIFIERS:
				setEndpointIdentifiers((EndpointIdentifiersType)newValue);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__TRANSFORMERS:
				setTransformers((TransformersType)newValue);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__GLOBAL_ENDPOINTS:
				setGlobalEndpoints((GlobalEndpointsType)newValue);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__INTERCEPTOR_STACK:
				getInterceptorStack().clear();
				getInterceptorStack().addAll((Collection)newValue);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__MODEL:
				setModel((ModelType)newValue);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__MULE_DESCRIPTOR:
				getMuleDescriptor().clear();
				getMuleDescriptor().addAll((Collection)newValue);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__ID:
				setId((String)newValue);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__VERSION:
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
			case MulePackage.MULE_CONFIGURATION_TYPE__MIXED:
				getMixed().clear();
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__DESCRIPTION:
				setDescription(DESCRIPTION_EDEFAULT);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__ENVIRONMENT_PROPERTIES:
				setEnvironmentProperties((EnvironmentPropertiesType)null);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__MULE_ENVIRONMENT_PROPERTIES:
				setMuleEnvironmentProperties((MuleEnvironmentPropertiesType)null);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__CONTAINER_CONTEXT:
				getContainerContext().clear();
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__SECURITY_MANAGER:
				setSecurityManager((SecurityManagerType)null);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__TRANSACTION_MANAGER:
				setTransactionManager((TransactionManagerType)null);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__AGENTS:
				setAgents((AgentsType)null);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__CONNECTOR:
				getConnector().clear();
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__ENDPOINT_IDENTIFIERS:
				setEndpointIdentifiers((EndpointIdentifiersType)null);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__TRANSFORMERS:
				setTransformers((TransformersType)null);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__GLOBAL_ENDPOINTS:
				setGlobalEndpoints((GlobalEndpointsType)null);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__INTERCEPTOR_STACK:
				getInterceptorStack().clear();
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__MODEL:
				setModel((ModelType)null);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__MULE_DESCRIPTOR:
				getMuleDescriptor().clear();
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__ID:
				setId(ID_EDEFAULT);
				return;
			case MulePackage.MULE_CONFIGURATION_TYPE__VERSION:
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
			case MulePackage.MULE_CONFIGURATION_TYPE__MIXED:
				return mixed != null && !mixed.isEmpty();
			case MulePackage.MULE_CONFIGURATION_TYPE__DESCRIPTION:
				return DESCRIPTION_EDEFAULT == null ? getDescription() != null : !DESCRIPTION_EDEFAULT.equals(getDescription());
			case MulePackage.MULE_CONFIGURATION_TYPE__ENVIRONMENT_PROPERTIES:
				return getEnvironmentProperties() != null;
			case MulePackage.MULE_CONFIGURATION_TYPE__MULE_ENVIRONMENT_PROPERTIES:
				return getMuleEnvironmentProperties() != null;
			case MulePackage.MULE_CONFIGURATION_TYPE__CONTAINER_CONTEXT:
				return !getContainerContext().isEmpty();
			case MulePackage.MULE_CONFIGURATION_TYPE__SECURITY_MANAGER:
				return getSecurityManager() != null;
			case MulePackage.MULE_CONFIGURATION_TYPE__TRANSACTION_MANAGER:
				return getTransactionManager() != null;
			case MulePackage.MULE_CONFIGURATION_TYPE__AGENTS:
				return getAgents() != null;
			case MulePackage.MULE_CONFIGURATION_TYPE__CONNECTOR:
				return !getConnector().isEmpty();
			case MulePackage.MULE_CONFIGURATION_TYPE__ENDPOINT_IDENTIFIERS:
				return getEndpointIdentifiers() != null;
			case MulePackage.MULE_CONFIGURATION_TYPE__TRANSFORMERS:
				return getTransformers() != null;
			case MulePackage.MULE_CONFIGURATION_TYPE__GLOBAL_ENDPOINTS:
				return getGlobalEndpoints() != null;
			case MulePackage.MULE_CONFIGURATION_TYPE__INTERCEPTOR_STACK:
				return !getInterceptorStack().isEmpty();
			case MulePackage.MULE_CONFIGURATION_TYPE__MODEL:
				return getModel() != null;
			case MulePackage.MULE_CONFIGURATION_TYPE__MULE_DESCRIPTOR:
				return !getMuleDescriptor().isEmpty();
			case MulePackage.MULE_CONFIGURATION_TYPE__ID:
				return ID_EDEFAULT == null ? id != null : !ID_EDEFAULT.equals(id);
			case MulePackage.MULE_CONFIGURATION_TYPE__VERSION:
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
		result.append(", id: ");
		result.append(id);
		result.append(", version: ");
		result.append(version);
		result.append(')');
		return result.toString();
	}

} //MuleConfigurationTypeImpl
