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

import org.mule.schema.ExhaustedActionType;
import org.mule.schema.InitialisationPolicyType;
import org.mule.schema.MulePackage;
import org.mule.schema.PoolingProfileType;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Pooling Profile Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.mule.schema.impl.PoolingProfileTypeImpl#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.impl.PoolingProfileTypeImpl#getExhaustedAction <em>Exhausted Action</em>}</li>
 *   <li>{@link org.mule.schema.impl.PoolingProfileTypeImpl#getFactory <em>Factory</em>}</li>
 *   <li>{@link org.mule.schema.impl.PoolingProfileTypeImpl#getInitialisationPolicy <em>Initialisation Policy</em>}</li>
 *   <li>{@link org.mule.schema.impl.PoolingProfileTypeImpl#getMaxActive <em>Max Active</em>}</li>
 *   <li>{@link org.mule.schema.impl.PoolingProfileTypeImpl#getMaxIdle <em>Max Idle</em>}</li>
 *   <li>{@link org.mule.schema.impl.PoolingProfileTypeImpl#getMaxWait <em>Max Wait</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class PoolingProfileTypeImpl extends EObjectImpl implements PoolingProfileType {
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
	 * The default value of the '{@link #getExhaustedAction() <em>Exhausted Action</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getExhaustedAction()
	 * @generated
	 * @ordered
	 */
	protected static final ExhaustedActionType EXHAUSTED_ACTION_EDEFAULT = ExhaustedActionType.GROW_LITERAL;

	/**
	 * The cached value of the '{@link #getExhaustedAction() <em>Exhausted Action</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getExhaustedAction()
	 * @generated
	 * @ordered
	 */
	protected ExhaustedActionType exhaustedAction = EXHAUSTED_ACTION_EDEFAULT;

	/**
	 * This is true if the Exhausted Action attribute has been set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	protected boolean exhaustedActionESet = false;

	/**
	 * The default value of the '{@link #getFactory() <em>Factory</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFactory()
	 * @generated
	 * @ordered
	 */
	protected static final String FACTORY_EDEFAULT = "org.mule.config.pool.CommonsPoolFactory";

	/**
	 * The cached value of the '{@link #getFactory() <em>Factory</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFactory()
	 * @generated
	 * @ordered
	 */
	protected String factory = FACTORY_EDEFAULT;

	/**
	 * This is true if the Factory attribute has been set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	protected boolean factoryESet = false;

	/**
	 * The default value of the '{@link #getInitialisationPolicy() <em>Initialisation Policy</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getInitialisationPolicy()
	 * @generated
	 * @ordered
	 */
	protected static final InitialisationPolicyType INITIALISATION_POLICY_EDEFAULT = InitialisationPolicyType.INITIALISE_FIRST_LITERAL;

	/**
	 * The cached value of the '{@link #getInitialisationPolicy() <em>Initialisation Policy</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getInitialisationPolicy()
	 * @generated
	 * @ordered
	 */
	protected InitialisationPolicyType initialisationPolicy = INITIALISATION_POLICY_EDEFAULT;

	/**
	 * This is true if the Initialisation Policy attribute has been set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	protected boolean initialisationPolicyESet = false;

	/**
	 * The default value of the '{@link #getMaxActive() <em>Max Active</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxActive()
	 * @generated
	 * @ordered
	 */
	protected static final String MAX_ACTIVE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getMaxActive() <em>Max Active</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxActive()
	 * @generated
	 * @ordered
	 */
	protected String maxActive = MAX_ACTIVE_EDEFAULT;

	/**
	 * The default value of the '{@link #getMaxIdle() <em>Max Idle</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxIdle()
	 * @generated
	 * @ordered
	 */
	protected static final String MAX_IDLE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getMaxIdle() <em>Max Idle</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxIdle()
	 * @generated
	 * @ordered
	 */
	protected String maxIdle = MAX_IDLE_EDEFAULT;

	/**
	 * The default value of the '{@link #getMaxWait() <em>Max Wait</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxWait()
	 * @generated
	 * @ordered
	 */
	protected static final String MAX_WAIT_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getMaxWait() <em>Max Wait</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxWait()
	 * @generated
	 * @ordered
	 */
	protected String maxWait = MAX_WAIT_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected PoolingProfileTypeImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EClass eStaticClass() {
		return MulePackage.eINSTANCE.getPoolingProfileType();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FeatureMap getMixed() {
		if (mixed == null) {
			mixed = new BasicFeatureMap(this, MulePackage.POOLING_PROFILE_TYPE__MIXED);
		}
		return mixed;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ExhaustedActionType getExhaustedAction() {
		return exhaustedAction;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setExhaustedAction(ExhaustedActionType newExhaustedAction) {
		ExhaustedActionType oldExhaustedAction = exhaustedAction;
		exhaustedAction = newExhaustedAction == null ? EXHAUSTED_ACTION_EDEFAULT : newExhaustedAction;
		boolean oldExhaustedActionESet = exhaustedActionESet;
		exhaustedActionESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.POOLING_PROFILE_TYPE__EXHAUSTED_ACTION, oldExhaustedAction, exhaustedAction, !oldExhaustedActionESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void unsetExhaustedAction() {
		ExhaustedActionType oldExhaustedAction = exhaustedAction;
		boolean oldExhaustedActionESet = exhaustedActionESet;
		exhaustedAction = EXHAUSTED_ACTION_EDEFAULT;
		exhaustedActionESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.POOLING_PROFILE_TYPE__EXHAUSTED_ACTION, oldExhaustedAction, EXHAUSTED_ACTION_EDEFAULT, oldExhaustedActionESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSetExhaustedAction() {
		return exhaustedActionESet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getFactory() {
		return factory;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setFactory(String newFactory) {
		String oldFactory = factory;
		factory = newFactory;
		boolean oldFactoryESet = factoryESet;
		factoryESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.POOLING_PROFILE_TYPE__FACTORY, oldFactory, factory, !oldFactoryESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void unsetFactory() {
		String oldFactory = factory;
		boolean oldFactoryESet = factoryESet;
		factory = FACTORY_EDEFAULT;
		factoryESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.POOLING_PROFILE_TYPE__FACTORY, oldFactory, FACTORY_EDEFAULT, oldFactoryESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSetFactory() {
		return factoryESet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public InitialisationPolicyType getInitialisationPolicy() {
		return initialisationPolicy;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setInitialisationPolicy(InitialisationPolicyType newInitialisationPolicy) {
		InitialisationPolicyType oldInitialisationPolicy = initialisationPolicy;
		initialisationPolicy = newInitialisationPolicy == null ? INITIALISATION_POLICY_EDEFAULT : newInitialisationPolicy;
		boolean oldInitialisationPolicyESet = initialisationPolicyESet;
		initialisationPolicyESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.POOLING_PROFILE_TYPE__INITIALISATION_POLICY, oldInitialisationPolicy, initialisationPolicy, !oldInitialisationPolicyESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void unsetInitialisationPolicy() {
		InitialisationPolicyType oldInitialisationPolicy = initialisationPolicy;
		boolean oldInitialisationPolicyESet = initialisationPolicyESet;
		initialisationPolicy = INITIALISATION_POLICY_EDEFAULT;
		initialisationPolicyESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.POOLING_PROFILE_TYPE__INITIALISATION_POLICY, oldInitialisationPolicy, INITIALISATION_POLICY_EDEFAULT, oldInitialisationPolicyESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSetInitialisationPolicy() {
		return initialisationPolicyESet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getMaxActive() {
		return maxActive;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setMaxActive(String newMaxActive) {
		String oldMaxActive = maxActive;
		maxActive = newMaxActive;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.POOLING_PROFILE_TYPE__MAX_ACTIVE, oldMaxActive, maxActive));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getMaxIdle() {
		return maxIdle;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setMaxIdle(String newMaxIdle) {
		String oldMaxIdle = maxIdle;
		maxIdle = newMaxIdle;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.POOLING_PROFILE_TYPE__MAX_IDLE, oldMaxIdle, maxIdle));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getMaxWait() {
		return maxWait;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setMaxWait(String newMaxWait) {
		String oldMaxWait = maxWait;
		maxWait = newMaxWait;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.POOLING_PROFILE_TYPE__MAX_WAIT, oldMaxWait, maxWait));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, Class baseClass, NotificationChain msgs) {
		if (featureID >= 0) {
			switch (eDerivedStructuralFeatureID(featureID, baseClass)) {
				case MulePackage.POOLING_PROFILE_TYPE__MIXED:
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
			case MulePackage.POOLING_PROFILE_TYPE__MIXED:
				return getMixed();
			case MulePackage.POOLING_PROFILE_TYPE__EXHAUSTED_ACTION:
				return getExhaustedAction();
			case MulePackage.POOLING_PROFILE_TYPE__FACTORY:
				return getFactory();
			case MulePackage.POOLING_PROFILE_TYPE__INITIALISATION_POLICY:
				return getInitialisationPolicy();
			case MulePackage.POOLING_PROFILE_TYPE__MAX_ACTIVE:
				return getMaxActive();
			case MulePackage.POOLING_PROFILE_TYPE__MAX_IDLE:
				return getMaxIdle();
			case MulePackage.POOLING_PROFILE_TYPE__MAX_WAIT:
				return getMaxWait();
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
			case MulePackage.POOLING_PROFILE_TYPE__MIXED:
				getMixed().clear();
				getMixed().addAll((Collection)newValue);
				return;
			case MulePackage.POOLING_PROFILE_TYPE__EXHAUSTED_ACTION:
				setExhaustedAction((ExhaustedActionType)newValue);
				return;
			case MulePackage.POOLING_PROFILE_TYPE__FACTORY:
				setFactory((String)newValue);
				return;
			case MulePackage.POOLING_PROFILE_TYPE__INITIALISATION_POLICY:
				setInitialisationPolicy((InitialisationPolicyType)newValue);
				return;
			case MulePackage.POOLING_PROFILE_TYPE__MAX_ACTIVE:
				setMaxActive((String)newValue);
				return;
			case MulePackage.POOLING_PROFILE_TYPE__MAX_IDLE:
				setMaxIdle((String)newValue);
				return;
			case MulePackage.POOLING_PROFILE_TYPE__MAX_WAIT:
				setMaxWait((String)newValue);
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
			case MulePackage.POOLING_PROFILE_TYPE__MIXED:
				getMixed().clear();
				return;
			case MulePackage.POOLING_PROFILE_TYPE__EXHAUSTED_ACTION:
				unsetExhaustedAction();
				return;
			case MulePackage.POOLING_PROFILE_TYPE__FACTORY:
				unsetFactory();
				return;
			case MulePackage.POOLING_PROFILE_TYPE__INITIALISATION_POLICY:
				unsetInitialisationPolicy();
				return;
			case MulePackage.POOLING_PROFILE_TYPE__MAX_ACTIVE:
				setMaxActive(MAX_ACTIVE_EDEFAULT);
				return;
			case MulePackage.POOLING_PROFILE_TYPE__MAX_IDLE:
				setMaxIdle(MAX_IDLE_EDEFAULT);
				return;
			case MulePackage.POOLING_PROFILE_TYPE__MAX_WAIT:
				setMaxWait(MAX_WAIT_EDEFAULT);
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
			case MulePackage.POOLING_PROFILE_TYPE__MIXED:
				return mixed != null && !mixed.isEmpty();
			case MulePackage.POOLING_PROFILE_TYPE__EXHAUSTED_ACTION:
				return isSetExhaustedAction();
			case MulePackage.POOLING_PROFILE_TYPE__FACTORY:
				return isSetFactory();
			case MulePackage.POOLING_PROFILE_TYPE__INITIALISATION_POLICY:
				return isSetInitialisationPolicy();
			case MulePackage.POOLING_PROFILE_TYPE__MAX_ACTIVE:
				return MAX_ACTIVE_EDEFAULT == null ? maxActive != null : !MAX_ACTIVE_EDEFAULT.equals(maxActive);
			case MulePackage.POOLING_PROFILE_TYPE__MAX_IDLE:
				return MAX_IDLE_EDEFAULT == null ? maxIdle != null : !MAX_IDLE_EDEFAULT.equals(maxIdle);
			case MulePackage.POOLING_PROFILE_TYPE__MAX_WAIT:
				return MAX_WAIT_EDEFAULT == null ? maxWait != null : !MAX_WAIT_EDEFAULT.equals(maxWait);
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
		result.append(", exhaustedAction: ");
		if (exhaustedActionESet) result.append(exhaustedAction); else result.append("<unset>");
		result.append(", factory: ");
		if (factoryESet) result.append(factory); else result.append("<unset>");
		result.append(", initialisationPolicy: ");
		if (initialisationPolicyESet) result.append(initialisationPolicy); else result.append("<unset>");
		result.append(", maxActive: ");
		result.append(maxActive);
		result.append(", maxIdle: ");
		result.append(maxIdle);
		result.append(", maxWait: ");
		result.append(maxWait);
		result.append(')');
		return result.toString();
	}

} //PoolingProfileTypeImpl
