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

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import org.eclipse.emf.ecore.util.BasicFeatureMap;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.ecore.util.InternalEList;

import org.mule.schema.IdType;
import org.mule.schema.MulePackage;
import org.mule.schema.PoolExhaustedActionType;
import org.mule.schema.ThreadingProfileType;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Threading Profile Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.mule.schema.impl.ThreadingProfileTypeImpl#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.impl.ThreadingProfileTypeImpl#isDoThreading <em>Do Threading</em>}</li>
 *   <li>{@link org.mule.schema.impl.ThreadingProfileTypeImpl#getId <em>Id</em>}</li>
 *   <li>{@link org.mule.schema.impl.ThreadingProfileTypeImpl#getMaxBufferSize <em>Max Buffer Size</em>}</li>
 *   <li>{@link org.mule.schema.impl.ThreadingProfileTypeImpl#getMaxThreadsActive <em>Max Threads Active</em>}</li>
 *   <li>{@link org.mule.schema.impl.ThreadingProfileTypeImpl#getMaxThreadsIdle <em>Max Threads Idle</em>}</li>
 *   <li>{@link org.mule.schema.impl.ThreadingProfileTypeImpl#getPoolExhaustedAction <em>Pool Exhausted Action</em>}</li>
 *   <li>{@link org.mule.schema.impl.ThreadingProfileTypeImpl#getThreadTTL <em>Thread TTL</em>}</li>
 *   <li>{@link org.mule.schema.impl.ThreadingProfileTypeImpl#getThreadWaitTimeout <em>Thread Wait Timeout</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ThreadingProfileTypeImpl extends EObjectImpl implements ThreadingProfileType {
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
	 * The default value of the '{@link #isDoThreading() <em>Do Threading</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isDoThreading()
	 * @generated
	 * @ordered
	 */
	protected static final boolean DO_THREADING_EDEFAULT = true;

	/**
	 * The cached value of the '{@link #isDoThreading() <em>Do Threading</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isDoThreading()
	 * @generated
	 * @ordered
	 */
	protected boolean doThreading = DO_THREADING_EDEFAULT;

	/**
	 * This is true if the Do Threading attribute has been set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	protected boolean doThreadingESet = false;

	/**
	 * The default value of the '{@link #getId() <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getId()
	 * @generated
	 * @ordered
	 */
	protected static final IdType ID_EDEFAULT = IdType.DEFAULT_LITERAL;

	/**
	 * The cached value of the '{@link #getId() <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getId()
	 * @generated
	 * @ordered
	 */
	protected IdType id = ID_EDEFAULT;

	/**
	 * This is true if the Id attribute has been set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	protected boolean idESet = false;

	/**
	 * The default value of the '{@link #getMaxBufferSize() <em>Max Buffer Size</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxBufferSize()
	 * @generated
	 * @ordered
	 */
	protected static final String MAX_BUFFER_SIZE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getMaxBufferSize() <em>Max Buffer Size</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxBufferSize()
	 * @generated
	 * @ordered
	 */
	protected String maxBufferSize = MAX_BUFFER_SIZE_EDEFAULT;

	/**
	 * The default value of the '{@link #getMaxThreadsActive() <em>Max Threads Active</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxThreadsActive()
	 * @generated
	 * @ordered
	 */
	protected static final String MAX_THREADS_ACTIVE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getMaxThreadsActive() <em>Max Threads Active</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxThreadsActive()
	 * @generated
	 * @ordered
	 */
	protected String maxThreadsActive = MAX_THREADS_ACTIVE_EDEFAULT;

	/**
	 * The default value of the '{@link #getMaxThreadsIdle() <em>Max Threads Idle</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxThreadsIdle()
	 * @generated
	 * @ordered
	 */
	protected static final String MAX_THREADS_IDLE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getMaxThreadsIdle() <em>Max Threads Idle</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxThreadsIdle()
	 * @generated
	 * @ordered
	 */
	protected String maxThreadsIdle = MAX_THREADS_IDLE_EDEFAULT;

	/**
	 * The default value of the '{@link #getPoolExhaustedAction() <em>Pool Exhausted Action</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPoolExhaustedAction()
	 * @generated
	 * @ordered
	 */
	protected static final PoolExhaustedActionType POOL_EXHAUSTED_ACTION_EDEFAULT = PoolExhaustedActionType.RUN_LITERAL;

	/**
	 * The cached value of the '{@link #getPoolExhaustedAction() <em>Pool Exhausted Action</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPoolExhaustedAction()
	 * @generated
	 * @ordered
	 */
	protected PoolExhaustedActionType poolExhaustedAction = POOL_EXHAUSTED_ACTION_EDEFAULT;

	/**
	 * This is true if the Pool Exhausted Action attribute has been set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	protected boolean poolExhaustedActionESet = false;

	/**
	 * The default value of the '{@link #getThreadTTL() <em>Thread TTL</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getThreadTTL()
	 * @generated
	 * @ordered
	 */
	protected static final String THREAD_TTL_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getThreadTTL() <em>Thread TTL</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getThreadTTL()
	 * @generated
	 * @ordered
	 */
	protected String threadTTL = THREAD_TTL_EDEFAULT;

	/**
	 * The default value of the '{@link #getThreadWaitTimeout() <em>Thread Wait Timeout</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getThreadWaitTimeout()
	 * @generated
	 * @ordered
	 */
	protected static final String THREAD_WAIT_TIMEOUT_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getThreadWaitTimeout() <em>Thread Wait Timeout</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getThreadWaitTimeout()
	 * @generated
	 * @ordered
	 */
	protected String threadWaitTimeout = THREAD_WAIT_TIMEOUT_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ThreadingProfileTypeImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EClass eStaticClass() {
		return MulePackage.eINSTANCE.getThreadingProfileType();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FeatureMap getMixed() {
		if (mixed == null) {
			mixed = new BasicFeatureMap(this, MulePackage.THREADING_PROFILE_TYPE__MIXED);
		}
		return mixed;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isDoThreading() {
		return doThreading;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setDoThreading(boolean newDoThreading) {
		boolean oldDoThreading = doThreading;
		doThreading = newDoThreading;
		boolean oldDoThreadingESet = doThreadingESet;
		doThreadingESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.THREADING_PROFILE_TYPE__DO_THREADING, oldDoThreading, doThreading, !oldDoThreadingESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void unsetDoThreading() {
		boolean oldDoThreading = doThreading;
		boolean oldDoThreadingESet = doThreadingESet;
		doThreading = DO_THREADING_EDEFAULT;
		doThreadingESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.THREADING_PROFILE_TYPE__DO_THREADING, oldDoThreading, DO_THREADING_EDEFAULT, oldDoThreadingESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSetDoThreading() {
		return doThreadingESet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public IdType getId() {
		return id;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setId(IdType newId) {
		IdType oldId = id;
		id = newId == null ? ID_EDEFAULT : newId;
		boolean oldIdESet = idESet;
		idESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.THREADING_PROFILE_TYPE__ID, oldId, id, !oldIdESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void unsetId() {
		IdType oldId = id;
		boolean oldIdESet = idESet;
		id = ID_EDEFAULT;
		idESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.THREADING_PROFILE_TYPE__ID, oldId, ID_EDEFAULT, oldIdESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSetId() {
		return idESet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getMaxBufferSize() {
		return maxBufferSize;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setMaxBufferSize(String newMaxBufferSize) {
		String oldMaxBufferSize = maxBufferSize;
		maxBufferSize = newMaxBufferSize;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.THREADING_PROFILE_TYPE__MAX_BUFFER_SIZE, oldMaxBufferSize, maxBufferSize));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getMaxThreadsActive() {
		return maxThreadsActive;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setMaxThreadsActive(String newMaxThreadsActive) {
		String oldMaxThreadsActive = maxThreadsActive;
		maxThreadsActive = newMaxThreadsActive;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.THREADING_PROFILE_TYPE__MAX_THREADS_ACTIVE, oldMaxThreadsActive, maxThreadsActive));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getMaxThreadsIdle() {
		return maxThreadsIdle;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setMaxThreadsIdle(String newMaxThreadsIdle) {
		String oldMaxThreadsIdle = maxThreadsIdle;
		maxThreadsIdle = newMaxThreadsIdle;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.THREADING_PROFILE_TYPE__MAX_THREADS_IDLE, oldMaxThreadsIdle, maxThreadsIdle));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PoolExhaustedActionType getPoolExhaustedAction() {
		return poolExhaustedAction;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setPoolExhaustedAction(PoolExhaustedActionType newPoolExhaustedAction) {
		PoolExhaustedActionType oldPoolExhaustedAction = poolExhaustedAction;
		poolExhaustedAction = newPoolExhaustedAction == null ? POOL_EXHAUSTED_ACTION_EDEFAULT : newPoolExhaustedAction;
		boolean oldPoolExhaustedActionESet = poolExhaustedActionESet;
		poolExhaustedActionESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.THREADING_PROFILE_TYPE__POOL_EXHAUSTED_ACTION, oldPoolExhaustedAction, poolExhaustedAction, !oldPoolExhaustedActionESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void unsetPoolExhaustedAction() {
		PoolExhaustedActionType oldPoolExhaustedAction = poolExhaustedAction;
		boolean oldPoolExhaustedActionESet = poolExhaustedActionESet;
		poolExhaustedAction = POOL_EXHAUSTED_ACTION_EDEFAULT;
		poolExhaustedActionESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.THREADING_PROFILE_TYPE__POOL_EXHAUSTED_ACTION, oldPoolExhaustedAction, POOL_EXHAUSTED_ACTION_EDEFAULT, oldPoolExhaustedActionESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSetPoolExhaustedAction() {
		return poolExhaustedActionESet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getThreadTTL() {
		return threadTTL;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setThreadTTL(String newThreadTTL) {
		String oldThreadTTL = threadTTL;
		threadTTL = newThreadTTL;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.THREADING_PROFILE_TYPE__THREAD_TTL, oldThreadTTL, threadTTL));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getThreadWaitTimeout() {
		return threadWaitTimeout;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setThreadWaitTimeout(String newThreadWaitTimeout) {
		String oldThreadWaitTimeout = threadWaitTimeout;
		threadWaitTimeout = newThreadWaitTimeout;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.THREADING_PROFILE_TYPE__THREAD_WAIT_TIMEOUT, oldThreadWaitTimeout, threadWaitTimeout));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, Class baseClass, NotificationChain msgs) {
		if (featureID >= 0) {
			switch (eDerivedStructuralFeatureID(featureID, baseClass)) {
				case MulePackage.THREADING_PROFILE_TYPE__MIXED:
					return ((InternalEList)getMixed()).basicRemove(otherEnd, msgs);
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
			case MulePackage.THREADING_PROFILE_TYPE__MIXED:
				return getMixed();
			case MulePackage.THREADING_PROFILE_TYPE__DO_THREADING:
				return isDoThreading() ? Boolean.TRUE : Boolean.FALSE;
			case MulePackage.THREADING_PROFILE_TYPE__ID:
				return getId();
			case MulePackage.THREADING_PROFILE_TYPE__MAX_BUFFER_SIZE:
				return getMaxBufferSize();
			case MulePackage.THREADING_PROFILE_TYPE__MAX_THREADS_ACTIVE:
				return getMaxThreadsActive();
			case MulePackage.THREADING_PROFILE_TYPE__MAX_THREADS_IDLE:
				return getMaxThreadsIdle();
			case MulePackage.THREADING_PROFILE_TYPE__POOL_EXHAUSTED_ACTION:
				return getPoolExhaustedAction();
			case MulePackage.THREADING_PROFILE_TYPE__THREAD_TTL:
				return getThreadTTL();
			case MulePackage.THREADING_PROFILE_TYPE__THREAD_WAIT_TIMEOUT:
				return getThreadWaitTimeout();
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
			case MulePackage.THREADING_PROFILE_TYPE__MIXED:
				getMixed().clear();
				getMixed().addAll((Collection)newValue);
				return;
			case MulePackage.THREADING_PROFILE_TYPE__DO_THREADING:
				setDoThreading(((Boolean)newValue).booleanValue());
				return;
			case MulePackage.THREADING_PROFILE_TYPE__ID:
				setId((IdType)newValue);
				return;
			case MulePackage.THREADING_PROFILE_TYPE__MAX_BUFFER_SIZE:
				setMaxBufferSize((String)newValue);
				return;
			case MulePackage.THREADING_PROFILE_TYPE__MAX_THREADS_ACTIVE:
				setMaxThreadsActive((String)newValue);
				return;
			case MulePackage.THREADING_PROFILE_TYPE__MAX_THREADS_IDLE:
				setMaxThreadsIdle((String)newValue);
				return;
			case MulePackage.THREADING_PROFILE_TYPE__POOL_EXHAUSTED_ACTION:
				setPoolExhaustedAction((PoolExhaustedActionType)newValue);
				return;
			case MulePackage.THREADING_PROFILE_TYPE__THREAD_TTL:
				setThreadTTL((String)newValue);
				return;
			case MulePackage.THREADING_PROFILE_TYPE__THREAD_WAIT_TIMEOUT:
				setThreadWaitTimeout((String)newValue);
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
			case MulePackage.THREADING_PROFILE_TYPE__MIXED:
				getMixed().clear();
				return;
			case MulePackage.THREADING_PROFILE_TYPE__DO_THREADING:
				unsetDoThreading();
				return;
			case MulePackage.THREADING_PROFILE_TYPE__ID:
				unsetId();
				return;
			case MulePackage.THREADING_PROFILE_TYPE__MAX_BUFFER_SIZE:
				setMaxBufferSize(MAX_BUFFER_SIZE_EDEFAULT);
				return;
			case MulePackage.THREADING_PROFILE_TYPE__MAX_THREADS_ACTIVE:
				setMaxThreadsActive(MAX_THREADS_ACTIVE_EDEFAULT);
				return;
			case MulePackage.THREADING_PROFILE_TYPE__MAX_THREADS_IDLE:
				setMaxThreadsIdle(MAX_THREADS_IDLE_EDEFAULT);
				return;
			case MulePackage.THREADING_PROFILE_TYPE__POOL_EXHAUSTED_ACTION:
				unsetPoolExhaustedAction();
				return;
			case MulePackage.THREADING_PROFILE_TYPE__THREAD_TTL:
				setThreadTTL(THREAD_TTL_EDEFAULT);
				return;
			case MulePackage.THREADING_PROFILE_TYPE__THREAD_WAIT_TIMEOUT:
				setThreadWaitTimeout(THREAD_WAIT_TIMEOUT_EDEFAULT);
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
			case MulePackage.THREADING_PROFILE_TYPE__MIXED:
				return mixed != null && !mixed.isEmpty();
			case MulePackage.THREADING_PROFILE_TYPE__DO_THREADING:
				return isSetDoThreading();
			case MulePackage.THREADING_PROFILE_TYPE__ID:
				return isSetId();
			case MulePackage.THREADING_PROFILE_TYPE__MAX_BUFFER_SIZE:
				return MAX_BUFFER_SIZE_EDEFAULT == null ? maxBufferSize != null : !MAX_BUFFER_SIZE_EDEFAULT.equals(maxBufferSize);
			case MulePackage.THREADING_PROFILE_TYPE__MAX_THREADS_ACTIVE:
				return MAX_THREADS_ACTIVE_EDEFAULT == null ? maxThreadsActive != null : !MAX_THREADS_ACTIVE_EDEFAULT.equals(maxThreadsActive);
			case MulePackage.THREADING_PROFILE_TYPE__MAX_THREADS_IDLE:
				return MAX_THREADS_IDLE_EDEFAULT == null ? maxThreadsIdle != null : !MAX_THREADS_IDLE_EDEFAULT.equals(maxThreadsIdle);
			case MulePackage.THREADING_PROFILE_TYPE__POOL_EXHAUSTED_ACTION:
				return isSetPoolExhaustedAction();
			case MulePackage.THREADING_PROFILE_TYPE__THREAD_TTL:
				return THREAD_TTL_EDEFAULT == null ? threadTTL != null : !THREAD_TTL_EDEFAULT.equals(threadTTL);
			case MulePackage.THREADING_PROFILE_TYPE__THREAD_WAIT_TIMEOUT:
				return THREAD_WAIT_TIMEOUT_EDEFAULT == null ? threadWaitTimeout != null : !THREAD_WAIT_TIMEOUT_EDEFAULT.equals(threadWaitTimeout);
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
		result.append(", doThreading: ");
		if (doThreadingESet) result.append(doThreading); else result.append("<unset>");
		result.append(", id: ");
		if (idESet) result.append(id); else result.append("<unset>");
		result.append(", maxBufferSize: ");
		result.append(maxBufferSize);
		result.append(", maxThreadsActive: ");
		result.append(maxThreadsActive);
		result.append(", maxThreadsIdle: ");
		result.append(maxThreadsIdle);
		result.append(", poolExhaustedAction: ");
		if (poolExhaustedActionESet) result.append(poolExhaustedAction); else result.append("<unset>");
		result.append(", threadTTL: ");
		result.append(threadTTL);
		result.append(", threadWaitTimeout: ");
		result.append(threadWaitTimeout);
		result.append(')');
		return result.toString();
	}

} //ThreadingProfileTypeImpl
