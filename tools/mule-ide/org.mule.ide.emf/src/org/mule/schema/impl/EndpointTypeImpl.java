/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.schema.impl;

import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import org.eclipse.emf.ecore.util.BasicFeatureMap;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.ecore.util.InternalEList;

import org.mule.schema.CreateConnectorType;
import org.mule.schema.EndpointType;
import org.mule.schema.FilterType;
import org.mule.schema.MulePackage;
import org.mule.schema.PropertiesType;
import org.mule.schema.SecurityFilterType;
import org.mule.schema.TransactionType;
import org.mule.schema.TypeType1;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Endpoint Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.mule.schema.impl.EndpointTypeImpl#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.impl.EndpointTypeImpl#getTransaction <em>Transaction</em>}</li>
 *   <li>{@link org.mule.schema.impl.EndpointTypeImpl#getFilter <em>Filter</em>}</li>
 *   <li>{@link org.mule.schema.impl.EndpointTypeImpl#getSecurityFilter <em>Security Filter</em>}</li>
 *   <li>{@link org.mule.schema.impl.EndpointTypeImpl#getProperties <em>Properties</em>}</li>
 *   <li>{@link org.mule.schema.impl.EndpointTypeImpl#getAddress <em>Address</em>}</li>
 *   <li>{@link org.mule.schema.impl.EndpointTypeImpl#getConnector <em>Connector</em>}</li>
 *   <li>{@link org.mule.schema.impl.EndpointTypeImpl#getCreateConnector <em>Create Connector</em>}</li>
 *   <li>{@link org.mule.schema.impl.EndpointTypeImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.mule.schema.impl.EndpointTypeImpl#getRef <em>Ref</em>}</li>
 *   <li>{@link org.mule.schema.impl.EndpointTypeImpl#isRemoteSync <em>Remote Sync</em>}</li>
 *   <li>{@link org.mule.schema.impl.EndpointTypeImpl#getRemoteSyncTimeout <em>Remote Sync Timeout</em>}</li>
 *   <li>{@link org.mule.schema.impl.EndpointTypeImpl#getResponseTransformers <em>Response Transformers</em>}</li>
 *   <li>{@link org.mule.schema.impl.EndpointTypeImpl#isSynchronous <em>Synchronous</em>}</li>
 *   <li>{@link org.mule.schema.impl.EndpointTypeImpl#getTransformers <em>Transformers</em>}</li>
 *   <li>{@link org.mule.schema.impl.EndpointTypeImpl#getType <em>Type</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class EndpointTypeImpl extends EObjectImpl implements EndpointType {
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
	 * The default value of the '{@link #getAddress() <em>Address</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAddress()
	 * @generated
	 * @ordered
	 */
	protected static final String ADDRESS_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getAddress() <em>Address</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAddress()
	 * @generated
	 * @ordered
	 */
	protected String address = ADDRESS_EDEFAULT;

	/**
	 * The default value of the '{@link #getConnector() <em>Connector</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getConnector()
	 * @generated
	 * @ordered
	 */
	protected static final String CONNECTOR_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getConnector() <em>Connector</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getConnector()
	 * @generated
	 * @ordered
	 */
	protected String connector = CONNECTOR_EDEFAULT;

	/**
	 * The default value of the '{@link #getCreateConnector() <em>Create Connector</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCreateConnector()
	 * @generated
	 * @ordered
	 */
	protected static final CreateConnectorType CREATE_CONNECTOR_EDEFAULT = CreateConnectorType.GET_OR_CREATE_LITERAL;

	/**
	 * The cached value of the '{@link #getCreateConnector() <em>Create Connector</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCreateConnector()
	 * @generated
	 * @ordered
	 */
	protected CreateConnectorType createConnector = CREATE_CONNECTOR_EDEFAULT;

	/**
	 * This is true if the Create Connector attribute has been set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	protected boolean createConnectorESet = false;

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
	 * The default value of the '{@link #isRemoteSync() <em>Remote Sync</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isRemoteSync()
	 * @generated
	 * @ordered
	 */
	protected static final boolean REMOTE_SYNC_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isRemoteSync() <em>Remote Sync</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isRemoteSync()
	 * @generated
	 * @ordered
	 */
	protected boolean remoteSync = REMOTE_SYNC_EDEFAULT;

	/**
	 * This is true if the Remote Sync attribute has been set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	protected boolean remoteSyncESet = false;

	/**
	 * The default value of the '{@link #getRemoteSyncTimeout() <em>Remote Sync Timeout</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRemoteSyncTimeout()
	 * @generated
	 * @ordered
	 */
	protected static final String REMOTE_SYNC_TIMEOUT_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getRemoteSyncTimeout() <em>Remote Sync Timeout</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRemoteSyncTimeout()
	 * @generated
	 * @ordered
	 */
	protected String remoteSyncTimeout = REMOTE_SYNC_TIMEOUT_EDEFAULT;

	/**
	 * The default value of the '{@link #getResponseTransformers() <em>Response Transformers</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getResponseTransformers()
	 * @generated
	 * @ordered
	 */
	protected static final List RESPONSE_TRANSFORMERS_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getResponseTransformers() <em>Response Transformers</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getResponseTransformers()
	 * @generated
	 * @ordered
	 */
	protected List responseTransformers = RESPONSE_TRANSFORMERS_EDEFAULT;

	/**
	 * The default value of the '{@link #isSynchronous() <em>Synchronous</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSynchronous()
	 * @generated
	 * @ordered
	 */
	protected static final boolean SYNCHRONOUS_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isSynchronous() <em>Synchronous</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSynchronous()
	 * @generated
	 * @ordered
	 */
	protected boolean synchronous = SYNCHRONOUS_EDEFAULT;

	/**
	 * This is true if the Synchronous attribute has been set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	protected boolean synchronousESet = false;

	/**
	 * The default value of the '{@link #getTransformers() <em>Transformers</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTransformers()
	 * @generated
	 * @ordered
	 */
	protected static final String TRANSFORMERS_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getTransformers() <em>Transformers</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTransformers()
	 * @generated
	 * @ordered
	 */
	protected String transformers = TRANSFORMERS_EDEFAULT;

	/**
	 * The default value of the '{@link #getType() <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getType()
	 * @generated
	 * @ordered
	 */
	protected static final TypeType1 TYPE_EDEFAULT = TypeType1.SENDER_AND_RECEIVER_LITERAL;

	/**
	 * The cached value of the '{@link #getType() <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getType()
	 * @generated
	 * @ordered
	 */
	protected TypeType1 type = TYPE_EDEFAULT;

	/**
	 * This is true if the Type attribute has been set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	protected boolean typeESet = false;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EndpointTypeImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EClass eStaticClass() {
		return MulePackage.eINSTANCE.getEndpointType();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FeatureMap getMixed() {
		if (mixed == null) {
			mixed = new BasicFeatureMap(this, MulePackage.ENDPOINT_TYPE__MIXED);
		}
		return mixed;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public TransactionType getTransaction() {
		return (TransactionType)getMixed().get(MulePackage.eINSTANCE.getEndpointType_Transaction(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetTransaction(TransactionType newTransaction, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getEndpointType_Transaction(), newTransaction, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setTransaction(TransactionType newTransaction) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getEndpointType_Transaction(), newTransaction);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FilterType getFilter() {
		return (FilterType)getMixed().get(MulePackage.eINSTANCE.getEndpointType_Filter(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetFilter(FilterType newFilter, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getEndpointType_Filter(), newFilter, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setFilter(FilterType newFilter) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getEndpointType_Filter(), newFilter);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public SecurityFilterType getSecurityFilter() {
		return (SecurityFilterType)getMixed().get(MulePackage.eINSTANCE.getEndpointType_SecurityFilter(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetSecurityFilter(SecurityFilterType newSecurityFilter, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getEndpointType_SecurityFilter(), newSecurityFilter, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setSecurityFilter(SecurityFilterType newSecurityFilter) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getEndpointType_SecurityFilter(), newSecurityFilter);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PropertiesType getProperties() {
		return (PropertiesType)getMixed().get(MulePackage.eINSTANCE.getEndpointType_Properties(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetProperties(PropertiesType newProperties, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getEndpointType_Properties(), newProperties, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setProperties(PropertiesType newProperties) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getEndpointType_Properties(), newProperties);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setAddress(String newAddress) {
		String oldAddress = address;
		address = newAddress;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.ENDPOINT_TYPE__ADDRESS, oldAddress, address));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getConnector() {
		return connector;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setConnector(String newConnector) {
		String oldConnector = connector;
		connector = newConnector;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.ENDPOINT_TYPE__CONNECTOR, oldConnector, connector));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public CreateConnectorType getCreateConnector() {
		return createConnector;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setCreateConnector(CreateConnectorType newCreateConnector) {
		CreateConnectorType oldCreateConnector = createConnector;
		createConnector = newCreateConnector == null ? CREATE_CONNECTOR_EDEFAULT : newCreateConnector;
		boolean oldCreateConnectorESet = createConnectorESet;
		createConnectorESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.ENDPOINT_TYPE__CREATE_CONNECTOR, oldCreateConnector, createConnector, !oldCreateConnectorESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void unsetCreateConnector() {
		CreateConnectorType oldCreateConnector = createConnector;
		boolean oldCreateConnectorESet = createConnectorESet;
		createConnector = CREATE_CONNECTOR_EDEFAULT;
		createConnectorESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.ENDPOINT_TYPE__CREATE_CONNECTOR, oldCreateConnector, CREATE_CONNECTOR_EDEFAULT, oldCreateConnectorESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSetCreateConnector() {
		return createConnectorESet;
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
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.ENDPOINT_TYPE__NAME, oldName, name));
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
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.ENDPOINT_TYPE__REF, oldRef, ref));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isRemoteSync() {
		return remoteSync;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setRemoteSync(boolean newRemoteSync) {
		boolean oldRemoteSync = remoteSync;
		remoteSync = newRemoteSync;
		boolean oldRemoteSyncESet = remoteSyncESet;
		remoteSyncESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.ENDPOINT_TYPE__REMOTE_SYNC, oldRemoteSync, remoteSync, !oldRemoteSyncESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void unsetRemoteSync() {
		boolean oldRemoteSync = remoteSync;
		boolean oldRemoteSyncESet = remoteSyncESet;
		remoteSync = REMOTE_SYNC_EDEFAULT;
		remoteSyncESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.ENDPOINT_TYPE__REMOTE_SYNC, oldRemoteSync, REMOTE_SYNC_EDEFAULT, oldRemoteSyncESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSetRemoteSync() {
		return remoteSyncESet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getRemoteSyncTimeout() {
		return remoteSyncTimeout;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setRemoteSyncTimeout(String newRemoteSyncTimeout) {
		String oldRemoteSyncTimeout = remoteSyncTimeout;
		remoteSyncTimeout = newRemoteSyncTimeout;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.ENDPOINT_TYPE__REMOTE_SYNC_TIMEOUT, oldRemoteSyncTimeout, remoteSyncTimeout));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public List getResponseTransformers() {
		return responseTransformers;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setResponseTransformers(List newResponseTransformers) {
		List oldResponseTransformers = responseTransformers;
		responseTransformers = newResponseTransformers;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.ENDPOINT_TYPE__RESPONSE_TRANSFORMERS, oldResponseTransformers, responseTransformers));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSynchronous() {
		return synchronous;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setSynchronous(boolean newSynchronous) {
		boolean oldSynchronous = synchronous;
		synchronous = newSynchronous;
		boolean oldSynchronousESet = synchronousESet;
		synchronousESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.ENDPOINT_TYPE__SYNCHRONOUS, oldSynchronous, synchronous, !oldSynchronousESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void unsetSynchronous() {
		boolean oldSynchronous = synchronous;
		boolean oldSynchronousESet = synchronousESet;
		synchronous = SYNCHRONOUS_EDEFAULT;
		synchronousESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.ENDPOINT_TYPE__SYNCHRONOUS, oldSynchronous, SYNCHRONOUS_EDEFAULT, oldSynchronousESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSetSynchronous() {
		return synchronousESet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getTransformers() {
		return transformers;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setTransformers(String newTransformers) {
		String oldTransformers = transformers;
		transformers = newTransformers;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.ENDPOINT_TYPE__TRANSFORMERS, oldTransformers, transformers));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public TypeType1 getType() {
		return type;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setType(TypeType1 newType) {
		TypeType1 oldType = type;
		type = newType == null ? TYPE_EDEFAULT : newType;
		boolean oldTypeESet = typeESet;
		typeESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.ENDPOINT_TYPE__TYPE, oldType, type, !oldTypeESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void unsetType() {
		TypeType1 oldType = type;
		boolean oldTypeESet = typeESet;
		type = TYPE_EDEFAULT;
		typeESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.ENDPOINT_TYPE__TYPE, oldType, TYPE_EDEFAULT, oldTypeESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSetType() {
		return typeESet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, Class baseClass, NotificationChain msgs) {
		if (featureID >= 0) {
			switch (eDerivedStructuralFeatureID(featureID, baseClass)) {
				case MulePackage.ENDPOINT_TYPE__MIXED:
					return ((InternalEList)getMixed()).basicRemove(otherEnd, msgs);
				case MulePackage.ENDPOINT_TYPE__TRANSACTION:
					return basicSetTransaction(null, msgs);
				case MulePackage.ENDPOINT_TYPE__FILTER:
					return basicSetFilter(null, msgs);
				case MulePackage.ENDPOINT_TYPE__SECURITY_FILTER:
					return basicSetSecurityFilter(null, msgs);
				case MulePackage.ENDPOINT_TYPE__PROPERTIES:
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
			case MulePackage.ENDPOINT_TYPE__MIXED:
				return getMixed();
			case MulePackage.ENDPOINT_TYPE__TRANSACTION:
				return getTransaction();
			case MulePackage.ENDPOINT_TYPE__FILTER:
				return getFilter();
			case MulePackage.ENDPOINT_TYPE__SECURITY_FILTER:
				return getSecurityFilter();
			case MulePackage.ENDPOINT_TYPE__PROPERTIES:
				return getProperties();
			case MulePackage.ENDPOINT_TYPE__ADDRESS:
				return getAddress();
			case MulePackage.ENDPOINT_TYPE__CONNECTOR:
				return getConnector();
			case MulePackage.ENDPOINT_TYPE__CREATE_CONNECTOR:
				return getCreateConnector();
			case MulePackage.ENDPOINT_TYPE__NAME:
				return getName();
			case MulePackage.ENDPOINT_TYPE__REF:
				return getRef();
			case MulePackage.ENDPOINT_TYPE__REMOTE_SYNC:
				return isRemoteSync() ? Boolean.TRUE : Boolean.FALSE;
			case MulePackage.ENDPOINT_TYPE__REMOTE_SYNC_TIMEOUT:
				return getRemoteSyncTimeout();
			case MulePackage.ENDPOINT_TYPE__RESPONSE_TRANSFORMERS:
				return getResponseTransformers();
			case MulePackage.ENDPOINT_TYPE__SYNCHRONOUS:
				return isSynchronous() ? Boolean.TRUE : Boolean.FALSE;
			case MulePackage.ENDPOINT_TYPE__TRANSFORMERS:
				return getTransformers();
			case MulePackage.ENDPOINT_TYPE__TYPE:
				return getType();
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
			case MulePackage.ENDPOINT_TYPE__MIXED:
				getMixed().clear();
				getMixed().addAll((Collection)newValue);
				return;
			case MulePackage.ENDPOINT_TYPE__TRANSACTION:
				setTransaction((TransactionType)newValue);
				return;
			case MulePackage.ENDPOINT_TYPE__FILTER:
				setFilter((FilterType)newValue);
				return;
			case MulePackage.ENDPOINT_TYPE__SECURITY_FILTER:
				setSecurityFilter((SecurityFilterType)newValue);
				return;
			case MulePackage.ENDPOINT_TYPE__PROPERTIES:
				setProperties((PropertiesType)newValue);
				return;
			case MulePackage.ENDPOINT_TYPE__ADDRESS:
				setAddress((String)newValue);
				return;
			case MulePackage.ENDPOINT_TYPE__CONNECTOR:
				setConnector((String)newValue);
				return;
			case MulePackage.ENDPOINT_TYPE__CREATE_CONNECTOR:
				setCreateConnector((CreateConnectorType)newValue);
				return;
			case MulePackage.ENDPOINT_TYPE__NAME:
				setName((String)newValue);
				return;
			case MulePackage.ENDPOINT_TYPE__REF:
				setRef((String)newValue);
				return;
			case MulePackage.ENDPOINT_TYPE__REMOTE_SYNC:
				setRemoteSync(((Boolean)newValue).booleanValue());
				return;
			case MulePackage.ENDPOINT_TYPE__REMOTE_SYNC_TIMEOUT:
				setRemoteSyncTimeout((String)newValue);
				return;
			case MulePackage.ENDPOINT_TYPE__RESPONSE_TRANSFORMERS:
				setResponseTransformers((List)newValue);
				return;
			case MulePackage.ENDPOINT_TYPE__SYNCHRONOUS:
				setSynchronous(((Boolean)newValue).booleanValue());
				return;
			case MulePackage.ENDPOINT_TYPE__TRANSFORMERS:
				setTransformers((String)newValue);
				return;
			case MulePackage.ENDPOINT_TYPE__TYPE:
				setType((TypeType1)newValue);
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
			case MulePackage.ENDPOINT_TYPE__MIXED:
				getMixed().clear();
				return;
			case MulePackage.ENDPOINT_TYPE__TRANSACTION:
				setTransaction((TransactionType)null);
				return;
			case MulePackage.ENDPOINT_TYPE__FILTER:
				setFilter((FilterType)null);
				return;
			case MulePackage.ENDPOINT_TYPE__SECURITY_FILTER:
				setSecurityFilter((SecurityFilterType)null);
				return;
			case MulePackage.ENDPOINT_TYPE__PROPERTIES:
				setProperties((PropertiesType)null);
				return;
			case MulePackage.ENDPOINT_TYPE__ADDRESS:
				setAddress(ADDRESS_EDEFAULT);
				return;
			case MulePackage.ENDPOINT_TYPE__CONNECTOR:
				setConnector(CONNECTOR_EDEFAULT);
				return;
			case MulePackage.ENDPOINT_TYPE__CREATE_CONNECTOR:
				unsetCreateConnector();
				return;
			case MulePackage.ENDPOINT_TYPE__NAME:
				setName(NAME_EDEFAULT);
				return;
			case MulePackage.ENDPOINT_TYPE__REF:
				setRef(REF_EDEFAULT);
				return;
			case MulePackage.ENDPOINT_TYPE__REMOTE_SYNC:
				unsetRemoteSync();
				return;
			case MulePackage.ENDPOINT_TYPE__REMOTE_SYNC_TIMEOUT:
				setRemoteSyncTimeout(REMOTE_SYNC_TIMEOUT_EDEFAULT);
				return;
			case MulePackage.ENDPOINT_TYPE__RESPONSE_TRANSFORMERS:
				setResponseTransformers(RESPONSE_TRANSFORMERS_EDEFAULT);
				return;
			case MulePackage.ENDPOINT_TYPE__SYNCHRONOUS:
				unsetSynchronous();
				return;
			case MulePackage.ENDPOINT_TYPE__TRANSFORMERS:
				setTransformers(TRANSFORMERS_EDEFAULT);
				return;
			case MulePackage.ENDPOINT_TYPE__TYPE:
				unsetType();
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
			case MulePackage.ENDPOINT_TYPE__MIXED:
				return mixed != null && !mixed.isEmpty();
			case MulePackage.ENDPOINT_TYPE__TRANSACTION:
				return getTransaction() != null;
			case MulePackage.ENDPOINT_TYPE__FILTER:
				return getFilter() != null;
			case MulePackage.ENDPOINT_TYPE__SECURITY_FILTER:
				return getSecurityFilter() != null;
			case MulePackage.ENDPOINT_TYPE__PROPERTIES:
				return getProperties() != null;
			case MulePackage.ENDPOINT_TYPE__ADDRESS:
				return ADDRESS_EDEFAULT == null ? address != null : !ADDRESS_EDEFAULT.equals(address);
			case MulePackage.ENDPOINT_TYPE__CONNECTOR:
				return CONNECTOR_EDEFAULT == null ? connector != null : !CONNECTOR_EDEFAULT.equals(connector);
			case MulePackage.ENDPOINT_TYPE__CREATE_CONNECTOR:
				return isSetCreateConnector();
			case MulePackage.ENDPOINT_TYPE__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case MulePackage.ENDPOINT_TYPE__REF:
				return REF_EDEFAULT == null ? ref != null : !REF_EDEFAULT.equals(ref);
			case MulePackage.ENDPOINT_TYPE__REMOTE_SYNC:
				return isSetRemoteSync();
			case MulePackage.ENDPOINT_TYPE__REMOTE_SYNC_TIMEOUT:
				return REMOTE_SYNC_TIMEOUT_EDEFAULT == null ? remoteSyncTimeout != null : !REMOTE_SYNC_TIMEOUT_EDEFAULT.equals(remoteSyncTimeout);
			case MulePackage.ENDPOINT_TYPE__RESPONSE_TRANSFORMERS:
				return RESPONSE_TRANSFORMERS_EDEFAULT == null ? responseTransformers != null : !RESPONSE_TRANSFORMERS_EDEFAULT.equals(responseTransformers);
			case MulePackage.ENDPOINT_TYPE__SYNCHRONOUS:
				return isSetSynchronous();
			case MulePackage.ENDPOINT_TYPE__TRANSFORMERS:
				return TRANSFORMERS_EDEFAULT == null ? transformers != null : !TRANSFORMERS_EDEFAULT.equals(transformers);
			case MulePackage.ENDPOINT_TYPE__TYPE:
				return isSetType();
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
		result.append(", address: ");
		result.append(address);
		result.append(", connector: ");
		result.append(connector);
		result.append(", createConnector: ");
		if (createConnectorESet) result.append(createConnector); else result.append("<unset>");
		result.append(", name: ");
		result.append(name);
		result.append(", ref: ");
		result.append(ref);
		result.append(", remoteSync: ");
		if (remoteSyncESet) result.append(remoteSync); else result.append("<unset>");
		result.append(", remoteSyncTimeout: ");
		result.append(remoteSyncTimeout);
		result.append(", responseTransformers: ");
		result.append(responseTransformers);
		result.append(", synchronous: ");
		if (synchronousESet) result.append(synchronous); else result.append("<unset>");
		result.append(", transformers: ");
		result.append(transformers);
		result.append(", type: ");
		if (typeESet) result.append(type); else result.append("<unset>");
		result.append(')');
		return result.toString();
	}

} //EndpointTypeImpl
