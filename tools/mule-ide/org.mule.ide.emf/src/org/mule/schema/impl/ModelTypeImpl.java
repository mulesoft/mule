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

import org.mule.schema.ComponentFactoryType;
import org.mule.schema.ComponentLifecycleAdapterFactoryType;
import org.mule.schema.ComponentPoolFactoryType;
import org.mule.schema.EntryPointResolverType;
import org.mule.schema.ExceptionStrategyType;
import org.mule.schema.ModelType;
import org.mule.schema.MulePackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Model Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.mule.schema.impl.ModelTypeImpl#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.impl.ModelTypeImpl#getDescription <em>Description</em>}</li>
 *   <li>{@link org.mule.schema.impl.ModelTypeImpl#getEntryPointResolver <em>Entry Point Resolver</em>}</li>
 *   <li>{@link org.mule.schema.impl.ModelTypeImpl#getComponentFactory <em>Component Factory</em>}</li>
 *   <li>{@link org.mule.schema.impl.ModelTypeImpl#getComponentLifecycleAdapterFactory <em>Component Lifecycle Adapter Factory</em>}</li>
 *   <li>{@link org.mule.schema.impl.ModelTypeImpl#getComponentPoolFactory <em>Component Pool Factory</em>}</li>
 *   <li>{@link org.mule.schema.impl.ModelTypeImpl#getExceptionStrategy <em>Exception Strategy</em>}</li>
 *   <li>{@link org.mule.schema.impl.ModelTypeImpl#getMuleDescriptor <em>Mule Descriptor</em>}</li>
 *   <li>{@link org.mule.schema.impl.ModelTypeImpl#getClassName <em>Class Name</em>}</li>
 *   <li>{@link org.mule.schema.impl.ModelTypeImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.mule.schema.impl.ModelTypeImpl#getRef <em>Ref</em>}</li>
 *   <li>{@link org.mule.schema.impl.ModelTypeImpl#getType <em>Type</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ModelTypeImpl extends EObjectImpl implements ModelType {
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
	 * The default value of the '{@link #getType() <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getType()
	 * @generated
	 * @ordered
	 */
	protected static final String TYPE_EDEFAULT = "seda";

	/**
	 * The cached value of the '{@link #getType() <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getType()
	 * @generated
	 * @ordered
	 */
	protected String type = TYPE_EDEFAULT;

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
	protected ModelTypeImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EClass eStaticClass() {
		return MulePackage.eINSTANCE.getModelType();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FeatureMap getMixed() {
		if (mixed == null) {
			mixed = new BasicFeatureMap(this, MulePackage.MODEL_TYPE__MIXED);
		}
		return mixed;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getDescription() {
		return (String)getMixed().get(MulePackage.eINSTANCE.getModelType_Description(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setDescription(String newDescription) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getModelType_Description(), newDescription);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EntryPointResolverType getEntryPointResolver() {
		return (EntryPointResolverType)getMixed().get(MulePackage.eINSTANCE.getModelType_EntryPointResolver(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetEntryPointResolver(EntryPointResolverType newEntryPointResolver, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getModelType_EntryPointResolver(), newEntryPointResolver, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setEntryPointResolver(EntryPointResolverType newEntryPointResolver) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getModelType_EntryPointResolver(), newEntryPointResolver);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ComponentFactoryType getComponentFactory() {
		return (ComponentFactoryType)getMixed().get(MulePackage.eINSTANCE.getModelType_ComponentFactory(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetComponentFactory(ComponentFactoryType newComponentFactory, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getModelType_ComponentFactory(), newComponentFactory, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setComponentFactory(ComponentFactoryType newComponentFactory) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getModelType_ComponentFactory(), newComponentFactory);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ComponentLifecycleAdapterFactoryType getComponentLifecycleAdapterFactory() {
		return (ComponentLifecycleAdapterFactoryType)getMixed().get(MulePackage.eINSTANCE.getModelType_ComponentLifecycleAdapterFactory(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetComponentLifecycleAdapterFactory(ComponentLifecycleAdapterFactoryType newComponentLifecycleAdapterFactory, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getModelType_ComponentLifecycleAdapterFactory(), newComponentLifecycleAdapterFactory, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setComponentLifecycleAdapterFactory(ComponentLifecycleAdapterFactoryType newComponentLifecycleAdapterFactory) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getModelType_ComponentLifecycleAdapterFactory(), newComponentLifecycleAdapterFactory);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ComponentPoolFactoryType getComponentPoolFactory() {
		return (ComponentPoolFactoryType)getMixed().get(MulePackage.eINSTANCE.getModelType_ComponentPoolFactory(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetComponentPoolFactory(ComponentPoolFactoryType newComponentPoolFactory, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getModelType_ComponentPoolFactory(), newComponentPoolFactory, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setComponentPoolFactory(ComponentPoolFactoryType newComponentPoolFactory) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getModelType_ComponentPoolFactory(), newComponentPoolFactory);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ExceptionStrategyType getExceptionStrategy() {
		return (ExceptionStrategyType)getMixed().get(MulePackage.eINSTANCE.getModelType_ExceptionStrategy(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetExceptionStrategy(ExceptionStrategyType newExceptionStrategy, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getModelType_ExceptionStrategy(), newExceptionStrategy, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setExceptionStrategy(ExceptionStrategyType newExceptionStrategy) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getModelType_ExceptionStrategy(), newExceptionStrategy);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getMuleDescriptor() {
		return ((FeatureMap)getMixed()).list(MulePackage.eINSTANCE.getModelType_MuleDescriptor());
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
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MODEL_TYPE__CLASS_NAME, oldClassName, className));
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
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MODEL_TYPE__NAME, oldName, name));
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
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MODEL_TYPE__REF, oldRef, ref));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getType() {
		return type;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setType(String newType) {
		String oldType = type;
		type = newType;
		boolean oldTypeESet = typeESet;
		typeESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.MODEL_TYPE__TYPE, oldType, type, !oldTypeESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void unsetType() {
		String oldType = type;
		boolean oldTypeESet = typeESet;
		type = TYPE_EDEFAULT;
		typeESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.MODEL_TYPE__TYPE, oldType, TYPE_EDEFAULT, oldTypeESet));
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
				case MulePackage.MODEL_TYPE__MIXED:
					return ((InternalEList)getMixed()).basicRemove(otherEnd, msgs);
				case MulePackage.MODEL_TYPE__ENTRY_POINT_RESOLVER:
					return basicSetEntryPointResolver(null, msgs);
				case MulePackage.MODEL_TYPE__COMPONENT_FACTORY:
					return basicSetComponentFactory(null, msgs);
				case MulePackage.MODEL_TYPE__COMPONENT_LIFECYCLE_ADAPTER_FACTORY:
					return basicSetComponentLifecycleAdapterFactory(null, msgs);
				case MulePackage.MODEL_TYPE__COMPONENT_POOL_FACTORY:
					return basicSetComponentPoolFactory(null, msgs);
				case MulePackage.MODEL_TYPE__EXCEPTION_STRATEGY:
					return basicSetExceptionStrategy(null, msgs);
				case MulePackage.MODEL_TYPE__MULE_DESCRIPTOR:
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
			case MulePackage.MODEL_TYPE__MIXED:
				return getMixed();
			case MulePackage.MODEL_TYPE__DESCRIPTION:
				return getDescription();
			case MulePackage.MODEL_TYPE__ENTRY_POINT_RESOLVER:
				return getEntryPointResolver();
			case MulePackage.MODEL_TYPE__COMPONENT_FACTORY:
				return getComponentFactory();
			case MulePackage.MODEL_TYPE__COMPONENT_LIFECYCLE_ADAPTER_FACTORY:
				return getComponentLifecycleAdapterFactory();
			case MulePackage.MODEL_TYPE__COMPONENT_POOL_FACTORY:
				return getComponentPoolFactory();
			case MulePackage.MODEL_TYPE__EXCEPTION_STRATEGY:
				return getExceptionStrategy();
			case MulePackage.MODEL_TYPE__MULE_DESCRIPTOR:
				return getMuleDescriptor();
			case MulePackage.MODEL_TYPE__CLASS_NAME:
				return getClassName();
			case MulePackage.MODEL_TYPE__NAME:
				return getName();
			case MulePackage.MODEL_TYPE__REF:
				return getRef();
			case MulePackage.MODEL_TYPE__TYPE:
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
			case MulePackage.MODEL_TYPE__MIXED:
				getMixed().clear();
				getMixed().addAll((Collection)newValue);
				return;
			case MulePackage.MODEL_TYPE__DESCRIPTION:
				setDescription((String)newValue);
				return;
			case MulePackage.MODEL_TYPE__ENTRY_POINT_RESOLVER:
				setEntryPointResolver((EntryPointResolverType)newValue);
				return;
			case MulePackage.MODEL_TYPE__COMPONENT_FACTORY:
				setComponentFactory((ComponentFactoryType)newValue);
				return;
			case MulePackage.MODEL_TYPE__COMPONENT_LIFECYCLE_ADAPTER_FACTORY:
				setComponentLifecycleAdapterFactory((ComponentLifecycleAdapterFactoryType)newValue);
				return;
			case MulePackage.MODEL_TYPE__COMPONENT_POOL_FACTORY:
				setComponentPoolFactory((ComponentPoolFactoryType)newValue);
				return;
			case MulePackage.MODEL_TYPE__EXCEPTION_STRATEGY:
				setExceptionStrategy((ExceptionStrategyType)newValue);
				return;
			case MulePackage.MODEL_TYPE__MULE_DESCRIPTOR:
				getMuleDescriptor().clear();
				getMuleDescriptor().addAll((Collection)newValue);
				return;
			case MulePackage.MODEL_TYPE__CLASS_NAME:
				setClassName((String)newValue);
				return;
			case MulePackage.MODEL_TYPE__NAME:
				setName((String)newValue);
				return;
			case MulePackage.MODEL_TYPE__REF:
				setRef((String)newValue);
				return;
			case MulePackage.MODEL_TYPE__TYPE:
				setType((String)newValue);
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
			case MulePackage.MODEL_TYPE__MIXED:
				getMixed().clear();
				return;
			case MulePackage.MODEL_TYPE__DESCRIPTION:
				setDescription(DESCRIPTION_EDEFAULT);
				return;
			case MulePackage.MODEL_TYPE__ENTRY_POINT_RESOLVER:
				setEntryPointResolver((EntryPointResolverType)null);
				return;
			case MulePackage.MODEL_TYPE__COMPONENT_FACTORY:
				setComponentFactory((ComponentFactoryType)null);
				return;
			case MulePackage.MODEL_TYPE__COMPONENT_LIFECYCLE_ADAPTER_FACTORY:
				setComponentLifecycleAdapterFactory((ComponentLifecycleAdapterFactoryType)null);
				return;
			case MulePackage.MODEL_TYPE__COMPONENT_POOL_FACTORY:
				setComponentPoolFactory((ComponentPoolFactoryType)null);
				return;
			case MulePackage.MODEL_TYPE__EXCEPTION_STRATEGY:
				setExceptionStrategy((ExceptionStrategyType)null);
				return;
			case MulePackage.MODEL_TYPE__MULE_DESCRIPTOR:
				getMuleDescriptor().clear();
				return;
			case MulePackage.MODEL_TYPE__CLASS_NAME:
				setClassName(CLASS_NAME_EDEFAULT);
				return;
			case MulePackage.MODEL_TYPE__NAME:
				setName(NAME_EDEFAULT);
				return;
			case MulePackage.MODEL_TYPE__REF:
				setRef(REF_EDEFAULT);
				return;
			case MulePackage.MODEL_TYPE__TYPE:
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
			case MulePackage.MODEL_TYPE__MIXED:
				return mixed != null && !mixed.isEmpty();
			case MulePackage.MODEL_TYPE__DESCRIPTION:
				return DESCRIPTION_EDEFAULT == null ? getDescription() != null : !DESCRIPTION_EDEFAULT.equals(getDescription());
			case MulePackage.MODEL_TYPE__ENTRY_POINT_RESOLVER:
				return getEntryPointResolver() != null;
			case MulePackage.MODEL_TYPE__COMPONENT_FACTORY:
				return getComponentFactory() != null;
			case MulePackage.MODEL_TYPE__COMPONENT_LIFECYCLE_ADAPTER_FACTORY:
				return getComponentLifecycleAdapterFactory() != null;
			case MulePackage.MODEL_TYPE__COMPONENT_POOL_FACTORY:
				return getComponentPoolFactory() != null;
			case MulePackage.MODEL_TYPE__EXCEPTION_STRATEGY:
				return getExceptionStrategy() != null;
			case MulePackage.MODEL_TYPE__MULE_DESCRIPTOR:
				return !getMuleDescriptor().isEmpty();
			case MulePackage.MODEL_TYPE__CLASS_NAME:
				return CLASS_NAME_EDEFAULT == null ? className != null : !CLASS_NAME_EDEFAULT.equals(className);
			case MulePackage.MODEL_TYPE__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case MulePackage.MODEL_TYPE__REF:
				return REF_EDEFAULT == null ? ref != null : !REF_EDEFAULT.equals(ref);
			case MulePackage.MODEL_TYPE__TYPE:
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
		result.append(", className: ");
		result.append(className);
		result.append(", name: ");
		result.append(name);
		result.append(", ref: ");
		result.append(ref);
		result.append(", type: ");
		if (typeESet) result.append(type); else result.append("<unset>");
		result.append(')');
		return result.toString();
	}

} //ModelTypeImpl
