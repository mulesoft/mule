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

import org.mule.schema.MulePackage;
import org.mule.schema.PropertiesType;
import org.mule.schema.TransformerType;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Transformer Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.mule.schema.impl.TransformerTypeImpl#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.impl.TransformerTypeImpl#getProperties <em>Properties</em>}</li>
 *   <li>{@link org.mule.schema.impl.TransformerTypeImpl#getClassName <em>Class Name</em>}</li>
 *   <li>{@link org.mule.schema.impl.TransformerTypeImpl#isIgnoreBadInput <em>Ignore Bad Input</em>}</li>
 *   <li>{@link org.mule.schema.impl.TransformerTypeImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.mule.schema.impl.TransformerTypeImpl#getRef <em>Ref</em>}</li>
 *   <li>{@link org.mule.schema.impl.TransformerTypeImpl#getReturnClass <em>Return Class</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class TransformerTypeImpl extends EObjectImpl implements TransformerType {
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
	 * The default value of the '{@link #isIgnoreBadInput() <em>Ignore Bad Input</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isIgnoreBadInput()
	 * @generated
	 * @ordered
	 */
	protected static final boolean IGNORE_BAD_INPUT_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isIgnoreBadInput() <em>Ignore Bad Input</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isIgnoreBadInput()
	 * @generated
	 * @ordered
	 */
	protected boolean ignoreBadInput = IGNORE_BAD_INPUT_EDEFAULT;

	/**
	 * This is true if the Ignore Bad Input attribute has been set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	protected boolean ignoreBadInputESet = false;

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
	 * The default value of the '{@link #getReturnClass() <em>Return Class</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getReturnClass()
	 * @generated
	 * @ordered
	 */
	protected static final String RETURN_CLASS_EDEFAULT = "java.lang.Object";

	/**
	 * The cached value of the '{@link #getReturnClass() <em>Return Class</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getReturnClass()
	 * @generated
	 * @ordered
	 */
	protected String returnClass = RETURN_CLASS_EDEFAULT;

	/**
	 * This is true if the Return Class attribute has been set.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	protected boolean returnClassESet = false;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected TransformerTypeImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EClass eStaticClass() {
		return MulePackage.eINSTANCE.getTransformerType();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FeatureMap getMixed() {
		if (mixed == null) {
			mixed = new BasicFeatureMap(this, MulePackage.TRANSFORMER_TYPE__MIXED);
		}
		return mixed;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PropertiesType getProperties() {
		return (PropertiesType)getMixed().get(MulePackage.eINSTANCE.getTransformerType_Properties(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetProperties(PropertiesType newProperties, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getTransformerType_Properties(), newProperties, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setProperties(PropertiesType newProperties) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getTransformerType_Properties(), newProperties);
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
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.TRANSFORMER_TYPE__CLASS_NAME, oldClassName, className));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isIgnoreBadInput() {
		return ignoreBadInput;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setIgnoreBadInput(boolean newIgnoreBadInput) {
		boolean oldIgnoreBadInput = ignoreBadInput;
		ignoreBadInput = newIgnoreBadInput;
		boolean oldIgnoreBadInputESet = ignoreBadInputESet;
		ignoreBadInputESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.TRANSFORMER_TYPE__IGNORE_BAD_INPUT, oldIgnoreBadInput, ignoreBadInput, !oldIgnoreBadInputESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void unsetIgnoreBadInput() {
		boolean oldIgnoreBadInput = ignoreBadInput;
		boolean oldIgnoreBadInputESet = ignoreBadInputESet;
		ignoreBadInput = IGNORE_BAD_INPUT_EDEFAULT;
		ignoreBadInputESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.TRANSFORMER_TYPE__IGNORE_BAD_INPUT, oldIgnoreBadInput, IGNORE_BAD_INPUT_EDEFAULT, oldIgnoreBadInputESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSetIgnoreBadInput() {
		return ignoreBadInputESet;
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
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.TRANSFORMER_TYPE__NAME, oldName, name));
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
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.TRANSFORMER_TYPE__REF, oldRef, ref));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getReturnClass() {
		return returnClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setReturnClass(String newReturnClass) {
		String oldReturnClass = returnClass;
		returnClass = newReturnClass;
		boolean oldReturnClassESet = returnClassESet;
		returnClassESet = true;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.TRANSFORMER_TYPE__RETURN_CLASS, oldReturnClass, returnClass, !oldReturnClassESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void unsetReturnClass() {
		String oldReturnClass = returnClass;
		boolean oldReturnClassESet = returnClassESet;
		returnClass = RETURN_CLASS_EDEFAULT;
		returnClassESet = false;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.UNSET, MulePackage.TRANSFORMER_TYPE__RETURN_CLASS, oldReturnClass, RETURN_CLASS_EDEFAULT, oldReturnClassESet));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean isSetReturnClass() {
		return returnClassESet;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, Class baseClass, NotificationChain msgs) {
		if (featureID >= 0) {
			switch (eDerivedStructuralFeatureID(featureID, baseClass)) {
				case MulePackage.TRANSFORMER_TYPE__MIXED:
					return ((InternalEList)getMixed()).basicRemove(otherEnd, msgs);
				case MulePackage.TRANSFORMER_TYPE__PROPERTIES:
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
			case MulePackage.TRANSFORMER_TYPE__MIXED:
				return getMixed();
			case MulePackage.TRANSFORMER_TYPE__PROPERTIES:
				return getProperties();
			case MulePackage.TRANSFORMER_TYPE__CLASS_NAME:
				return getClassName();
			case MulePackage.TRANSFORMER_TYPE__IGNORE_BAD_INPUT:
				return isIgnoreBadInput() ? Boolean.TRUE : Boolean.FALSE;
			case MulePackage.TRANSFORMER_TYPE__NAME:
				return getName();
			case MulePackage.TRANSFORMER_TYPE__REF:
				return getRef();
			case MulePackage.TRANSFORMER_TYPE__RETURN_CLASS:
				return getReturnClass();
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
			case MulePackage.TRANSFORMER_TYPE__MIXED:
				getMixed().clear();
				getMixed().addAll((Collection)newValue);
				return;
			case MulePackage.TRANSFORMER_TYPE__PROPERTIES:
				setProperties((PropertiesType)newValue);
				return;
			case MulePackage.TRANSFORMER_TYPE__CLASS_NAME:
				setClassName((String)newValue);
				return;
			case MulePackage.TRANSFORMER_TYPE__IGNORE_BAD_INPUT:
				setIgnoreBadInput(((Boolean)newValue).booleanValue());
				return;
			case MulePackage.TRANSFORMER_TYPE__NAME:
				setName((String)newValue);
				return;
			case MulePackage.TRANSFORMER_TYPE__REF:
				setRef((String)newValue);
				return;
			case MulePackage.TRANSFORMER_TYPE__RETURN_CLASS:
				setReturnClass((String)newValue);
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
			case MulePackage.TRANSFORMER_TYPE__MIXED:
				getMixed().clear();
				return;
			case MulePackage.TRANSFORMER_TYPE__PROPERTIES:
				setProperties((PropertiesType)null);
				return;
			case MulePackage.TRANSFORMER_TYPE__CLASS_NAME:
				setClassName(CLASS_NAME_EDEFAULT);
				return;
			case MulePackage.TRANSFORMER_TYPE__IGNORE_BAD_INPUT:
				unsetIgnoreBadInput();
				return;
			case MulePackage.TRANSFORMER_TYPE__NAME:
				setName(NAME_EDEFAULT);
				return;
			case MulePackage.TRANSFORMER_TYPE__REF:
				setRef(REF_EDEFAULT);
				return;
			case MulePackage.TRANSFORMER_TYPE__RETURN_CLASS:
				unsetReturnClass();
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
			case MulePackage.TRANSFORMER_TYPE__MIXED:
				return mixed != null && !mixed.isEmpty();
			case MulePackage.TRANSFORMER_TYPE__PROPERTIES:
				return getProperties() != null;
			case MulePackage.TRANSFORMER_TYPE__CLASS_NAME:
				return CLASS_NAME_EDEFAULT == null ? className != null : !CLASS_NAME_EDEFAULT.equals(className);
			case MulePackage.TRANSFORMER_TYPE__IGNORE_BAD_INPUT:
				return isSetIgnoreBadInput();
			case MulePackage.TRANSFORMER_TYPE__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case MulePackage.TRANSFORMER_TYPE__REF:
				return REF_EDEFAULT == null ? ref != null : !REF_EDEFAULT.equals(ref);
			case MulePackage.TRANSFORMER_TYPE__RETURN_CLASS:
				return isSetReturnClass();
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
		result.append(", ignoreBadInput: ");
		if (ignoreBadInputESet) result.append(ignoreBadInput); else result.append("<unset>");
		result.append(", name: ");
		result.append(name);
		result.append(", ref: ");
		result.append(ref);
		result.append(", returnClass: ");
		if (returnClassESet) result.append(returnClass); else result.append("<unset>");
		result.append(')');
		return result.toString();
	}

} //TransformerTypeImpl
