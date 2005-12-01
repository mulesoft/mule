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

import org.mule.schema.ListType;
import org.mule.schema.SchemaPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>List Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.mule.schema.impl.ListTypeImpl#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.impl.ListTypeImpl#getGroup <em>Group</em>}</li>
 *   <li>{@link org.mule.schema.impl.ListTypeImpl#getEntry <em>Entry</em>}</li>
 *   <li>{@link org.mule.schema.impl.ListTypeImpl#getFactoryEntry <em>Factory Entry</em>}</li>
 *   <li>{@link org.mule.schema.impl.ListTypeImpl#getSystemEntry <em>System Entry</em>}</li>
 *   <li>{@link org.mule.schema.impl.ListTypeImpl#getContainerEntry <em>Container Entry</em>}</li>
 *   <li>{@link org.mule.schema.impl.ListTypeImpl#getName <em>Name</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ListTypeImpl extends EObjectImpl implements ListType {
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
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ListTypeImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EClass eStaticClass() {
		return SchemaPackage.eINSTANCE.getListType();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FeatureMap getMixed() {
		if (mixed == null) {
			mixed = new BasicFeatureMap(this, SchemaPackage.LIST_TYPE__MIXED);
		}
		return mixed;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FeatureMap getGroup() {
		return (FeatureMap)((FeatureMap)getMixed()).list(SchemaPackage.eINSTANCE.getListType_Group());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getEntry() {
		return ((FeatureMap)getGroup()).list(SchemaPackage.eINSTANCE.getListType_Entry());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getFactoryEntry() {
		return ((FeatureMap)getGroup()).list(SchemaPackage.eINSTANCE.getListType_FactoryEntry());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getSystemEntry() {
		return ((FeatureMap)getGroup()).list(SchemaPackage.eINSTANCE.getListType_SystemEntry());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getContainerEntry() {
		return ((FeatureMap)getGroup()).list(SchemaPackage.eINSTANCE.getListType_ContainerEntry());
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
			eNotify(new ENotificationImpl(this, Notification.SET, SchemaPackage.LIST_TYPE__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, Class baseClass, NotificationChain msgs) {
		if (featureID >= 0) {
			switch (eDerivedStructuralFeatureID(featureID, baseClass)) {
				case SchemaPackage.LIST_TYPE__MIXED:
					return ((InternalEList)getMixed()).basicRemove(otherEnd, msgs);
				case SchemaPackage.LIST_TYPE__GROUP:
					return ((InternalEList)getGroup()).basicRemove(otherEnd, msgs);
				case SchemaPackage.LIST_TYPE__ENTRY:
					return ((InternalEList)getEntry()).basicRemove(otherEnd, msgs);
				case SchemaPackage.LIST_TYPE__FACTORY_ENTRY:
					return ((InternalEList)getFactoryEntry()).basicRemove(otherEnd, msgs);
				case SchemaPackage.LIST_TYPE__SYSTEM_ENTRY:
					return ((InternalEList)getSystemEntry()).basicRemove(otherEnd, msgs);
				case SchemaPackage.LIST_TYPE__CONTAINER_ENTRY:
					return ((InternalEList)getContainerEntry()).basicRemove(otherEnd, msgs);
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
			case SchemaPackage.LIST_TYPE__MIXED:
				return getMixed();
			case SchemaPackage.LIST_TYPE__GROUP:
				return getGroup();
			case SchemaPackage.LIST_TYPE__ENTRY:
				return getEntry();
			case SchemaPackage.LIST_TYPE__FACTORY_ENTRY:
				return getFactoryEntry();
			case SchemaPackage.LIST_TYPE__SYSTEM_ENTRY:
				return getSystemEntry();
			case SchemaPackage.LIST_TYPE__CONTAINER_ENTRY:
				return getContainerEntry();
			case SchemaPackage.LIST_TYPE__NAME:
				return getName();
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
			case SchemaPackage.LIST_TYPE__MIXED:
				getMixed().clear();
				getMixed().addAll((Collection)newValue);
				return;
			case SchemaPackage.LIST_TYPE__GROUP:
				getGroup().clear();
				getGroup().addAll((Collection)newValue);
				return;
			case SchemaPackage.LIST_TYPE__ENTRY:
				getEntry().clear();
				getEntry().addAll((Collection)newValue);
				return;
			case SchemaPackage.LIST_TYPE__FACTORY_ENTRY:
				getFactoryEntry().clear();
				getFactoryEntry().addAll((Collection)newValue);
				return;
			case SchemaPackage.LIST_TYPE__SYSTEM_ENTRY:
				getSystemEntry().clear();
				getSystemEntry().addAll((Collection)newValue);
				return;
			case SchemaPackage.LIST_TYPE__CONTAINER_ENTRY:
				getContainerEntry().clear();
				getContainerEntry().addAll((Collection)newValue);
				return;
			case SchemaPackage.LIST_TYPE__NAME:
				setName((String)newValue);
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
			case SchemaPackage.LIST_TYPE__MIXED:
				getMixed().clear();
				return;
			case SchemaPackage.LIST_TYPE__GROUP:
				getGroup().clear();
				return;
			case SchemaPackage.LIST_TYPE__ENTRY:
				getEntry().clear();
				return;
			case SchemaPackage.LIST_TYPE__FACTORY_ENTRY:
				getFactoryEntry().clear();
				return;
			case SchemaPackage.LIST_TYPE__SYSTEM_ENTRY:
				getSystemEntry().clear();
				return;
			case SchemaPackage.LIST_TYPE__CONTAINER_ENTRY:
				getContainerEntry().clear();
				return;
			case SchemaPackage.LIST_TYPE__NAME:
				setName(NAME_EDEFAULT);
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
			case SchemaPackage.LIST_TYPE__MIXED:
				return mixed != null && !mixed.isEmpty();
			case SchemaPackage.LIST_TYPE__GROUP:
				return !getGroup().isEmpty();
			case SchemaPackage.LIST_TYPE__ENTRY:
				return !getEntry().isEmpty();
			case SchemaPackage.LIST_TYPE__FACTORY_ENTRY:
				return !getFactoryEntry().isEmpty();
			case SchemaPackage.LIST_TYPE__SYSTEM_ENTRY:
				return !getSystemEntry().isEmpty();
			case SchemaPackage.LIST_TYPE__CONTAINER_ENTRY:
				return !getContainerEntry().isEmpty();
			case SchemaPackage.LIST_TYPE__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
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
		result.append(", name: ");
		result.append(name);
		result.append(')');
		return result.toString();
	}

} //ListTypeImpl
