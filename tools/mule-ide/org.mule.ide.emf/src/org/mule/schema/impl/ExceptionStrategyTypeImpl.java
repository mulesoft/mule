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
import org.mule.schema.MulePackage;
import org.mule.schema.PropertiesType;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Exception Strategy Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.mule.schema.impl.ExceptionStrategyTypeImpl#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.impl.ExceptionStrategyTypeImpl#getEndpoint <em>Endpoint</em>}</li>
 *   <li>{@link org.mule.schema.impl.ExceptionStrategyTypeImpl#getGlobalEndpoint <em>Global Endpoint</em>}</li>
 *   <li>{@link org.mule.schema.impl.ExceptionStrategyTypeImpl#getProperties <em>Properties</em>}</li>
 *   <li>{@link org.mule.schema.impl.ExceptionStrategyTypeImpl#getClassName <em>Class Name</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ExceptionStrategyTypeImpl extends EObjectImpl implements ExceptionStrategyType {
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
	 * The default value of the '{@link #getClassName() <em>Class Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getClassName()
	 * @generated
	 * @ordered
	 */
	protected static final String CLASS_NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getClassName() <em>Class Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getClassName()
	 * @generated
	 * @ordered
	 */
	protected String className = CLASS_NAME_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ExceptionStrategyTypeImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EClass eStaticClass() {
		return MulePackage.eINSTANCE.getExceptionStrategyType();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FeatureMap getMixed() {
		if (mixed == null) {
			mixed = new BasicFeatureMap(this, MulePackage.EXCEPTION_STRATEGY_TYPE__MIXED);
		}
		return mixed;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getEndpoint() {
		return ((FeatureMap)getMixed()).list(MulePackage.eINSTANCE.getExceptionStrategyType_Endpoint());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getGlobalEndpoint() {
		return ((FeatureMap)getMixed()).list(MulePackage.eINSTANCE.getExceptionStrategyType_GlobalEndpoint());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PropertiesType getProperties() {
		return (PropertiesType)getMixed().get(MulePackage.eINSTANCE.getExceptionStrategyType_Properties(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetProperties(PropertiesType newProperties, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getExceptionStrategyType_Properties(), newProperties, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setProperties(PropertiesType newProperties) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getExceptionStrategyType_Properties(), newProperties);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setClassName(String newClassName) {
		String oldClassName = className;
		className = newClassName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.EXCEPTION_STRATEGY_TYPE__CLASS_NAME, oldClassName, className));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, Class baseClass, NotificationChain msgs) {
		if (featureID >= 0) {
			switch (eDerivedStructuralFeatureID(featureID, baseClass)) {
				case MulePackage.EXCEPTION_STRATEGY_TYPE__MIXED:
					return ((InternalEList)getMixed()).basicRemove(otherEnd, msgs);
				case MulePackage.EXCEPTION_STRATEGY_TYPE__ENDPOINT:
					return ((InternalEList)getEndpoint()).basicRemove(otherEnd, msgs);
				case MulePackage.EXCEPTION_STRATEGY_TYPE__GLOBAL_ENDPOINT:
					return ((InternalEList)getGlobalEndpoint()).basicRemove(otherEnd, msgs);
				case MulePackage.EXCEPTION_STRATEGY_TYPE__PROPERTIES:
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
			case MulePackage.EXCEPTION_STRATEGY_TYPE__MIXED:
				return getMixed();
			case MulePackage.EXCEPTION_STRATEGY_TYPE__ENDPOINT:
				return getEndpoint();
			case MulePackage.EXCEPTION_STRATEGY_TYPE__GLOBAL_ENDPOINT:
				return getGlobalEndpoint();
			case MulePackage.EXCEPTION_STRATEGY_TYPE__PROPERTIES:
				return getProperties();
			case MulePackage.EXCEPTION_STRATEGY_TYPE__CLASS_NAME:
				return getClassName();
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
			case MulePackage.EXCEPTION_STRATEGY_TYPE__MIXED:
				getMixed().clear();
				getMixed().addAll((Collection)newValue);
				return;
			case MulePackage.EXCEPTION_STRATEGY_TYPE__ENDPOINT:
				getEndpoint().clear();
				getEndpoint().addAll((Collection)newValue);
				return;
			case MulePackage.EXCEPTION_STRATEGY_TYPE__GLOBAL_ENDPOINT:
				getGlobalEndpoint().clear();
				getGlobalEndpoint().addAll((Collection)newValue);
				return;
			case MulePackage.EXCEPTION_STRATEGY_TYPE__PROPERTIES:
				setProperties((PropertiesType)newValue);
				return;
			case MulePackage.EXCEPTION_STRATEGY_TYPE__CLASS_NAME:
				setClassName((String)newValue);
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
			case MulePackage.EXCEPTION_STRATEGY_TYPE__MIXED:
				getMixed().clear();
				return;
			case MulePackage.EXCEPTION_STRATEGY_TYPE__ENDPOINT:
				getEndpoint().clear();
				return;
			case MulePackage.EXCEPTION_STRATEGY_TYPE__GLOBAL_ENDPOINT:
				getGlobalEndpoint().clear();
				return;
			case MulePackage.EXCEPTION_STRATEGY_TYPE__PROPERTIES:
				setProperties((PropertiesType)null);
				return;
			case MulePackage.EXCEPTION_STRATEGY_TYPE__CLASS_NAME:
				setClassName(CLASS_NAME_EDEFAULT);
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
			case MulePackage.EXCEPTION_STRATEGY_TYPE__MIXED:
				return mixed != null && !mixed.isEmpty();
			case MulePackage.EXCEPTION_STRATEGY_TYPE__ENDPOINT:
				return !getEndpoint().isEmpty();
			case MulePackage.EXCEPTION_STRATEGY_TYPE__GLOBAL_ENDPOINT:
				return !getGlobalEndpoint().isEmpty();
			case MulePackage.EXCEPTION_STRATEGY_TYPE__PROPERTIES:
				return getProperties() != null;
			case MulePackage.EXCEPTION_STRATEGY_TYPE__CLASS_NAME:
				return CLASS_NAME_EDEFAULT == null ? className != null : !CLASS_NAME_EDEFAULT.equals(className);
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
		result.append(", className: ");
		result.append(className);
		result.append(')');
		return result.toString();
	}

} //ExceptionStrategyTypeImpl
