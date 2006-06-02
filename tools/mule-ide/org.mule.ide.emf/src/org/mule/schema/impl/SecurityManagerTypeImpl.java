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

import org.mule.schema.MulePackage;
import org.mule.schema.SecurityManagerType;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Security Manager Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.mule.schema.impl.SecurityManagerTypeImpl#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.impl.SecurityManagerTypeImpl#getSecurityProvider <em>Security Provider</em>}</li>
 *   <li>{@link org.mule.schema.impl.SecurityManagerTypeImpl#getEncryptionStrategy <em>Encryption Strategy</em>}</li>
 *   <li>{@link org.mule.schema.impl.SecurityManagerTypeImpl#getClassName <em>Class Name</em>}</li>
 *   <li>{@link org.mule.schema.impl.SecurityManagerTypeImpl#getRef <em>Ref</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class SecurityManagerTypeImpl extends EObjectImpl implements SecurityManagerType {
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
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected SecurityManagerTypeImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EClass eStaticClass() {
		return MulePackage.eINSTANCE.getSecurityManagerType();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FeatureMap getMixed() {
		if (mixed == null) {
			mixed = new BasicFeatureMap(this, MulePackage.SECURITY_MANAGER_TYPE__MIXED);
		}
		return mixed;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getSecurityProvider() {
		return ((FeatureMap)getMixed()).list(MulePackage.eINSTANCE.getSecurityManagerType_SecurityProvider());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getEncryptionStrategy() {
		return ((FeatureMap)getMixed()).list(MulePackage.eINSTANCE.getSecurityManagerType_EncryptionStrategy());
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
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.SECURITY_MANAGER_TYPE__CLASS_NAME, oldClassName, className));
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
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.SECURITY_MANAGER_TYPE__REF, oldRef, ref));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, Class baseClass, NotificationChain msgs) {
		if (featureID >= 0) {
			switch (eDerivedStructuralFeatureID(featureID, baseClass)) {
				case MulePackage.SECURITY_MANAGER_TYPE__MIXED:
					return ((InternalEList)getMixed()).basicRemove(otherEnd, msgs);
				case MulePackage.SECURITY_MANAGER_TYPE__SECURITY_PROVIDER:
					return ((InternalEList)getSecurityProvider()).basicRemove(otherEnd, msgs);
				case MulePackage.SECURITY_MANAGER_TYPE__ENCRYPTION_STRATEGY:
					return ((InternalEList)getEncryptionStrategy()).basicRemove(otherEnd, msgs);
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
			case MulePackage.SECURITY_MANAGER_TYPE__MIXED:
				return getMixed();
			case MulePackage.SECURITY_MANAGER_TYPE__SECURITY_PROVIDER:
				return getSecurityProvider();
			case MulePackage.SECURITY_MANAGER_TYPE__ENCRYPTION_STRATEGY:
				return getEncryptionStrategy();
			case MulePackage.SECURITY_MANAGER_TYPE__CLASS_NAME:
				return getClassName();
			case MulePackage.SECURITY_MANAGER_TYPE__REF:
				return getRef();
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
			case MulePackage.SECURITY_MANAGER_TYPE__MIXED:
				getMixed().clear();
				getMixed().addAll((Collection)newValue);
				return;
			case MulePackage.SECURITY_MANAGER_TYPE__SECURITY_PROVIDER:
				getSecurityProvider().clear();
				getSecurityProvider().addAll((Collection)newValue);
				return;
			case MulePackage.SECURITY_MANAGER_TYPE__ENCRYPTION_STRATEGY:
				getEncryptionStrategy().clear();
				getEncryptionStrategy().addAll((Collection)newValue);
				return;
			case MulePackage.SECURITY_MANAGER_TYPE__CLASS_NAME:
				setClassName((String)newValue);
				return;
			case MulePackage.SECURITY_MANAGER_TYPE__REF:
				setRef((String)newValue);
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
			case MulePackage.SECURITY_MANAGER_TYPE__MIXED:
				getMixed().clear();
				return;
			case MulePackage.SECURITY_MANAGER_TYPE__SECURITY_PROVIDER:
				getSecurityProvider().clear();
				return;
			case MulePackage.SECURITY_MANAGER_TYPE__ENCRYPTION_STRATEGY:
				getEncryptionStrategy().clear();
				return;
			case MulePackage.SECURITY_MANAGER_TYPE__CLASS_NAME:
				setClassName(CLASS_NAME_EDEFAULT);
				return;
			case MulePackage.SECURITY_MANAGER_TYPE__REF:
				setRef(REF_EDEFAULT);
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
			case MulePackage.SECURITY_MANAGER_TYPE__MIXED:
				return mixed != null && !mixed.isEmpty();
			case MulePackage.SECURITY_MANAGER_TYPE__SECURITY_PROVIDER:
				return !getSecurityProvider().isEmpty();
			case MulePackage.SECURITY_MANAGER_TYPE__ENCRYPTION_STRATEGY:
				return !getEncryptionStrategy().isEmpty();
			case MulePackage.SECURITY_MANAGER_TYPE__CLASS_NAME:
				return CLASS_NAME_EDEFAULT == null ? className != null : !CLASS_NAME_EDEFAULT.equals(className);
			case MulePackage.SECURITY_MANAGER_TYPE__REF:
				return REF_EDEFAULT == null ? ref != null : !REF_EDEFAULT.equals(ref);
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
		result.append(", ref: ");
		result.append(ref);
		result.append(')');
		return result.toString();
	}

} //SecurityManagerTypeImpl
