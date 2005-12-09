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

import org.mule.schema.FilterType;
import org.mule.schema.GlobalEndpointType;
import org.mule.schema.MulePackage;
import org.mule.schema.SecurityFilterType;
import org.mule.schema.TransactionType;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Global Endpoint Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.mule.schema.impl.GlobalEndpointTypeImpl#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.impl.GlobalEndpointTypeImpl#getTransaction <em>Transaction</em>}</li>
 *   <li>{@link org.mule.schema.impl.GlobalEndpointTypeImpl#getFilter <em>Filter</em>}</li>
 *   <li>{@link org.mule.schema.impl.GlobalEndpointTypeImpl#getSecurityFilter <em>Security Filter</em>}</li>
 *   <li>{@link org.mule.schema.impl.GlobalEndpointTypeImpl#getProperties <em>Properties</em>}</li>
 *   <li>{@link org.mule.schema.impl.GlobalEndpointTypeImpl#getAddress <em>Address</em>}</li>
 *   <li>{@link org.mule.schema.impl.GlobalEndpointTypeImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.mule.schema.impl.GlobalEndpointTypeImpl#isRemoteSync <em>Remote Sync</em>}</li>
 *   <li>{@link org.mule.schema.impl.GlobalEndpointTypeImpl#getRemoteSyncTimeout <em>Remote Sync Timeout</em>}</li>
 *   <li>{@link org.mule.schema.impl.GlobalEndpointTypeImpl#getResponseTransformers <em>Response Transformers</em>}</li>
 *   <li>{@link org.mule.schema.impl.GlobalEndpointTypeImpl#isSynchronous <em>Synchronous</em>}</li>
 *   <li>{@link org.mule.schema.impl.GlobalEndpointTypeImpl#getTransformers <em>Transformers</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class GlobalEndpointTypeImpl extends EObjectImpl implements GlobalEndpointType {
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
	protected static final String RESPONSE_TRANSFORMERS_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getResponseTransformers() <em>Response Transformers</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getResponseTransformers()
	 * @generated
	 * @ordered
	 */
	protected String responseTransformers = RESPONSE_TRANSFORMERS_EDEFAULT;

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
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected GlobalEndpointTypeImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EClass eStaticClass() {
		return MulePackage.eINSTANCE.getGlobalEndpointType();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FeatureMap getMixed() {
		if (mixed == null) {
			mixed = new BasicFeatureMap(this, MulePackage.GLOBAL_ENDPOINT_TYPE__MIXED);
		}
		return mixed;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public TransactionType getTransaction() {
		return (TransactionType)getMixed().get(MulePackage.eINSTANCE.getGlobalEndpointType_Transaction(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetTransaction(TransactionType newTransaction, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getGlobalEndpointType_Transaction(), newTransaction, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setTransaction(TransactionType newTransaction) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getGlobalEndpointType_Transaction(), newTransaction);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FilterType getFilter() {
		return (FilterType)getMixed().get(MulePackage.eINSTANCE.getGlobalEndpointType_Filter(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetFilter(FilterType newFilter, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getGlobalEndpointType_Filter(), newFilter, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setFilter(FilterType newFilter) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getGlobalEndpointType_Filter(), newFilter);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public SecurityFilterType getSecurityFilter() {
		return (SecurityFilterType)getMixed().get(MulePackage.eINSTANCE.getGlobalEndpointType_SecurityFilter(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetSecurityFilter(SecurityFilterType newSecurityFilter, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getGlobalEndpointType_SecurityFilter(), newSecurityFilter, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setSecurityFilter(SecurityFilterType newSecurityFilter) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getGlobalEndpointType_SecurityFilter(), newSecurityFilter);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getProperties() {
		return ((FeatureMap)getMixed()).list(MulePackage.eINSTANCE.getGlobalEndpointType_Properties());
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
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.GLOBAL_ENDPOINT_TYPE__ADDRESS, oldAddress, address));
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
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.GLOBAL_ENDPOINT_TYPE__NAME, oldName, name));
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
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.GLOBAL_ENDPOINT_TYPE__REMOTE_SYNC, oldRemoteSync, remoteSync, !oldRemoteSyncESet));
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
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.GLOBAL_ENDPOINT_TYPE__REMOTE_SYNC, oldRemoteSync, REMOTE_SYNC_EDEFAULT, oldRemoteSyncESet));
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
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.GLOBAL_ENDPOINT_TYPE__REMOTE_SYNC_TIMEOUT, oldRemoteSyncTimeout, remoteSyncTimeout));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getResponseTransformers() {
		return responseTransformers;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setResponseTransformers(String newResponseTransformers) {
		String oldResponseTransformers = responseTransformers;
		responseTransformers = newResponseTransformers;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.GLOBAL_ENDPOINT_TYPE__RESPONSE_TRANSFORMERS, oldResponseTransformers, responseTransformers));
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
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.GLOBAL_ENDPOINT_TYPE__SYNCHRONOUS, oldSynchronous, synchronous, !oldSynchronousESet));
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
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.GLOBAL_ENDPOINT_TYPE__SYNCHRONOUS, oldSynchronous, SYNCHRONOUS_EDEFAULT, oldSynchronousESet));
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
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.GLOBAL_ENDPOINT_TYPE__TRANSFORMERS, oldTransformers, transformers));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, Class baseClass, NotificationChain msgs) {
		if (featureID >= 0) {
			switch (eDerivedStructuralFeatureID(featureID, baseClass)) {
				case MulePackage.GLOBAL_ENDPOINT_TYPE__MIXED:
					return ((InternalEList)getMixed()).basicRemove(otherEnd, msgs);
				case MulePackage.GLOBAL_ENDPOINT_TYPE__TRANSACTION:
					return basicSetTransaction(null, msgs);
				case MulePackage.GLOBAL_ENDPOINT_TYPE__FILTER:
					return basicSetFilter(null, msgs);
				case MulePackage.GLOBAL_ENDPOINT_TYPE__SECURITY_FILTER:
					return basicSetSecurityFilter(null, msgs);
				case MulePackage.GLOBAL_ENDPOINT_TYPE__PROPERTIES:
					return ((InternalEList)getProperties()).basicRemove(otherEnd, msgs);
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
			case MulePackage.GLOBAL_ENDPOINT_TYPE__MIXED:
				return getMixed();
			case MulePackage.GLOBAL_ENDPOINT_TYPE__TRANSACTION:
				return getTransaction();
			case MulePackage.GLOBAL_ENDPOINT_TYPE__FILTER:
				return getFilter();
			case MulePackage.GLOBAL_ENDPOINT_TYPE__SECURITY_FILTER:
				return getSecurityFilter();
			case MulePackage.GLOBAL_ENDPOINT_TYPE__PROPERTIES:
				return getProperties();
			case MulePackage.GLOBAL_ENDPOINT_TYPE__ADDRESS:
				return getAddress();
			case MulePackage.GLOBAL_ENDPOINT_TYPE__NAME:
				return getName();
			case MulePackage.GLOBAL_ENDPOINT_TYPE__REMOTE_SYNC:
				return isRemoteSync() ? Boolean.TRUE : Boolean.FALSE;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__REMOTE_SYNC_TIMEOUT:
				return getRemoteSyncTimeout();
			case MulePackage.GLOBAL_ENDPOINT_TYPE__RESPONSE_TRANSFORMERS:
				return getResponseTransformers();
			case MulePackage.GLOBAL_ENDPOINT_TYPE__SYNCHRONOUS:
				return isSynchronous() ? Boolean.TRUE : Boolean.FALSE;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__TRANSFORMERS:
				return getTransformers();
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
			case MulePackage.GLOBAL_ENDPOINT_TYPE__MIXED:
				getMixed().clear();
				getMixed().addAll((Collection)newValue);
				return;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__TRANSACTION:
				setTransaction((TransactionType)newValue);
				return;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__FILTER:
				setFilter((FilterType)newValue);
				return;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__SECURITY_FILTER:
				setSecurityFilter((SecurityFilterType)newValue);
				return;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__PROPERTIES:
				getProperties().clear();
				getProperties().addAll((Collection)newValue);
				return;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__ADDRESS:
				setAddress((String)newValue);
				return;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__NAME:
				setName((String)newValue);
				return;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__REMOTE_SYNC:
				setRemoteSync(((Boolean)newValue).booleanValue());
				return;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__REMOTE_SYNC_TIMEOUT:
				setRemoteSyncTimeout((String)newValue);
				return;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__RESPONSE_TRANSFORMERS:
				setResponseTransformers((String)newValue);
				return;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__SYNCHRONOUS:
				setSynchronous(((Boolean)newValue).booleanValue());
				return;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__TRANSFORMERS:
				setTransformers((String)newValue);
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
			case MulePackage.GLOBAL_ENDPOINT_TYPE__MIXED:
				getMixed().clear();
				return;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__TRANSACTION:
				setTransaction((TransactionType)null);
				return;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__FILTER:
				setFilter((FilterType)null);
				return;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__SECURITY_FILTER:
				setSecurityFilter((SecurityFilterType)null);
				return;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__PROPERTIES:
				getProperties().clear();
				return;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__ADDRESS:
				setAddress(ADDRESS_EDEFAULT);
				return;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__NAME:
				setName(NAME_EDEFAULT);
				return;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__REMOTE_SYNC:
				unsetRemoteSync();
				return;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__REMOTE_SYNC_TIMEOUT:
				setRemoteSyncTimeout(REMOTE_SYNC_TIMEOUT_EDEFAULT);
				return;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__RESPONSE_TRANSFORMERS:
				setResponseTransformers(RESPONSE_TRANSFORMERS_EDEFAULT);
				return;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__SYNCHRONOUS:
				unsetSynchronous();
				return;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__TRANSFORMERS:
				setTransformers(TRANSFORMERS_EDEFAULT);
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
			case MulePackage.GLOBAL_ENDPOINT_TYPE__MIXED:
				return mixed != null && !mixed.isEmpty();
			case MulePackage.GLOBAL_ENDPOINT_TYPE__TRANSACTION:
				return getTransaction() != null;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__FILTER:
				return getFilter() != null;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__SECURITY_FILTER:
				return getSecurityFilter() != null;
			case MulePackage.GLOBAL_ENDPOINT_TYPE__PROPERTIES:
				return !getProperties().isEmpty();
			case MulePackage.GLOBAL_ENDPOINT_TYPE__ADDRESS:
				return ADDRESS_EDEFAULT == null ? address != null : !ADDRESS_EDEFAULT.equals(address);
			case MulePackage.GLOBAL_ENDPOINT_TYPE__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case MulePackage.GLOBAL_ENDPOINT_TYPE__REMOTE_SYNC:
				return isSetRemoteSync();
			case MulePackage.GLOBAL_ENDPOINT_TYPE__REMOTE_SYNC_TIMEOUT:
				return REMOTE_SYNC_TIMEOUT_EDEFAULT == null ? remoteSyncTimeout != null : !REMOTE_SYNC_TIMEOUT_EDEFAULT.equals(remoteSyncTimeout);
			case MulePackage.GLOBAL_ENDPOINT_TYPE__RESPONSE_TRANSFORMERS:
				return RESPONSE_TRANSFORMERS_EDEFAULT == null ? responseTransformers != null : !RESPONSE_TRANSFORMERS_EDEFAULT.equals(responseTransformers);
			case MulePackage.GLOBAL_ENDPOINT_TYPE__SYNCHRONOUS:
				return isSetSynchronous();
			case MulePackage.GLOBAL_ENDPOINT_TYPE__TRANSFORMERS:
				return TRANSFORMERS_EDEFAULT == null ? transformers != null : !TRANSFORMERS_EDEFAULT.equals(transformers);
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
		result.append(", name: ");
		result.append(name);
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
		result.append(')');
		return result.toString();
	}

} //GlobalEndpointTypeImpl
