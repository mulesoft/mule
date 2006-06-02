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

import org.mule.schema.FilterType;
import org.mule.schema.LeftFilterType;
import org.mule.schema.MulePackage;
import org.mule.schema.PropertiesType;
import org.mule.schema.RightFilterType;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Filter Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.mule.schema.impl.FilterTypeImpl#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.impl.FilterTypeImpl#getProperties <em>Properties</em>}</li>
 *   <li>{@link org.mule.schema.impl.FilterTypeImpl#getFilter <em>Filter</em>}</li>
 *   <li>{@link org.mule.schema.impl.FilterTypeImpl#getLeftFilter <em>Left Filter</em>}</li>
 *   <li>{@link org.mule.schema.impl.FilterTypeImpl#getRightFilter <em>Right Filter</em>}</li>
 *   <li>{@link org.mule.schema.impl.FilterTypeImpl#getClassName <em>Class Name</em>}</li>
 *   <li>{@link org.mule.schema.impl.FilterTypeImpl#getConfigFile <em>Config File</em>}</li>
 *   <li>{@link org.mule.schema.impl.FilterTypeImpl#getExpectedType <em>Expected Type</em>}</li>
 *   <li>{@link org.mule.schema.impl.FilterTypeImpl#getExpression <em>Expression</em>}</li>
 *   <li>{@link org.mule.schema.impl.FilterTypeImpl#getPath <em>Path</em>}</li>
 *   <li>{@link org.mule.schema.impl.FilterTypeImpl#getPattern <em>Pattern</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class FilterTypeImpl extends EObjectImpl implements FilterType {
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
	 * The default value of the '{@link #getConfigFile() <em>Config File</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getConfigFile()
	 * @generated
	 * @ordered
	 */
	protected static final String CONFIG_FILE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getConfigFile() <em>Config File</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getConfigFile()
	 * @generated
	 * @ordered
	 */
	protected String configFile = CONFIG_FILE_EDEFAULT;

	/**
	 * The default value of the '{@link #getExpectedType() <em>Expected Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getExpectedType()
	 * @generated
	 * @ordered
	 */
	protected static final String EXPECTED_TYPE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getExpectedType() <em>Expected Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getExpectedType()
	 * @generated
	 * @ordered
	 */
	protected String expectedType = EXPECTED_TYPE_EDEFAULT;

	/**
	 * The default value of the '{@link #getExpression() <em>Expression</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getExpression()
	 * @generated
	 * @ordered
	 */
	protected static final String EXPRESSION_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getExpression() <em>Expression</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getExpression()
	 * @generated
	 * @ordered
	 */
	protected String expression = EXPRESSION_EDEFAULT;

	/**
	 * The default value of the '{@link #getPath() <em>Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPath()
	 * @generated
	 * @ordered
	 */
	protected static final String PATH_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getPath() <em>Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPath()
	 * @generated
	 * @ordered
	 */
	protected String path = PATH_EDEFAULT;

	/**
	 * The default value of the '{@link #getPattern() <em>Pattern</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPattern()
	 * @generated
	 * @ordered
	 */
	protected static final String PATTERN_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getPattern() <em>Pattern</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPattern()
	 * @generated
	 * @ordered
	 */
	protected String pattern = PATTERN_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected FilterTypeImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EClass eStaticClass() {
		return MulePackage.eINSTANCE.getFilterType();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FeatureMap getMixed() {
		if (mixed == null) {
			mixed = new BasicFeatureMap(this, MulePackage.FILTER_TYPE__MIXED);
		}
		return mixed;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public PropertiesType getProperties() {
		return (PropertiesType)getMixed().get(MulePackage.eINSTANCE.getFilterType_Properties(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetProperties(PropertiesType newProperties, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getFilterType_Properties(), newProperties, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setProperties(PropertiesType newProperties) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getFilterType_Properties(), newProperties);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FilterType getFilter() {
		return (FilterType)getMixed().get(MulePackage.eINSTANCE.getFilterType_Filter(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetFilter(FilterType newFilter, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getFilterType_Filter(), newFilter, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setFilter(FilterType newFilter) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getFilterType_Filter(), newFilter);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public LeftFilterType getLeftFilter() {
		return (LeftFilterType)getMixed().get(MulePackage.eINSTANCE.getFilterType_LeftFilter(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetLeftFilter(LeftFilterType newLeftFilter, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getFilterType_LeftFilter(), newLeftFilter, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setLeftFilter(LeftFilterType newLeftFilter) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getFilterType_LeftFilter(), newLeftFilter);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public RightFilterType getRightFilter() {
		return (RightFilterType)getMixed().get(MulePackage.eINSTANCE.getFilterType_RightFilter(), true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetRightFilter(RightFilterType newRightFilter, NotificationChain msgs) {
		return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getFilterType_RightFilter(), newRightFilter, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setRightFilter(RightFilterType newRightFilter) {
		((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getFilterType_RightFilter(), newRightFilter);
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
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.FILTER_TYPE__CLASS_NAME, oldClassName, className));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getConfigFile() {
		return configFile;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setConfigFile(String newConfigFile) {
		String oldConfigFile = configFile;
		configFile = newConfigFile;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.FILTER_TYPE__CONFIG_FILE, oldConfigFile, configFile));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getExpectedType() {
		return expectedType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setExpectedType(String newExpectedType) {
		String oldExpectedType = expectedType;
		expectedType = newExpectedType;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.FILTER_TYPE__EXPECTED_TYPE, oldExpectedType, expectedType));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getExpression() {
		return expression;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setExpression(String newExpression) {
		String oldExpression = expression;
		expression = newExpression;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.FILTER_TYPE__EXPRESSION, oldExpression, expression));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getPath() {
		return path;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setPath(String newPath) {
		String oldPath = path;
		path = newPath;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.FILTER_TYPE__PATH, oldPath, path));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getPattern() {
		return pattern;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setPattern(String newPattern) {
		String oldPattern = pattern;
		pattern = newPattern;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.FILTER_TYPE__PATTERN, oldPattern, pattern));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, Class baseClass, NotificationChain msgs) {
		if (featureID >= 0) {
			switch (eDerivedStructuralFeatureID(featureID, baseClass)) {
				case MulePackage.FILTER_TYPE__MIXED:
					return ((InternalEList)getMixed()).basicRemove(otherEnd, msgs);
				case MulePackage.FILTER_TYPE__PROPERTIES:
					return basicSetProperties(null, msgs);
				case MulePackage.FILTER_TYPE__FILTER:
					return basicSetFilter(null, msgs);
				case MulePackage.FILTER_TYPE__LEFT_FILTER:
					return basicSetLeftFilter(null, msgs);
				case MulePackage.FILTER_TYPE__RIGHT_FILTER:
					return basicSetRightFilter(null, msgs);
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
			case MulePackage.FILTER_TYPE__MIXED:
				return getMixed();
			case MulePackage.FILTER_TYPE__PROPERTIES:
				return getProperties();
			case MulePackage.FILTER_TYPE__FILTER:
				return getFilter();
			case MulePackage.FILTER_TYPE__LEFT_FILTER:
				return getLeftFilter();
			case MulePackage.FILTER_TYPE__RIGHT_FILTER:
				return getRightFilter();
			case MulePackage.FILTER_TYPE__CLASS_NAME:
				return getClassName();
			case MulePackage.FILTER_TYPE__CONFIG_FILE:
				return getConfigFile();
			case MulePackage.FILTER_TYPE__EXPECTED_TYPE:
				return getExpectedType();
			case MulePackage.FILTER_TYPE__EXPRESSION:
				return getExpression();
			case MulePackage.FILTER_TYPE__PATH:
				return getPath();
			case MulePackage.FILTER_TYPE__PATTERN:
				return getPattern();
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
			case MulePackage.FILTER_TYPE__MIXED:
				getMixed().clear();
				getMixed().addAll((Collection)newValue);
				return;
			case MulePackage.FILTER_TYPE__PROPERTIES:
				setProperties((PropertiesType)newValue);
				return;
			case MulePackage.FILTER_TYPE__FILTER:
				setFilter((FilterType)newValue);
				return;
			case MulePackage.FILTER_TYPE__LEFT_FILTER:
				setLeftFilter((LeftFilterType)newValue);
				return;
			case MulePackage.FILTER_TYPE__RIGHT_FILTER:
				setRightFilter((RightFilterType)newValue);
				return;
			case MulePackage.FILTER_TYPE__CLASS_NAME:
				setClassName((String)newValue);
				return;
			case MulePackage.FILTER_TYPE__CONFIG_FILE:
				setConfigFile((String)newValue);
				return;
			case MulePackage.FILTER_TYPE__EXPECTED_TYPE:
				setExpectedType((String)newValue);
				return;
			case MulePackage.FILTER_TYPE__EXPRESSION:
				setExpression((String)newValue);
				return;
			case MulePackage.FILTER_TYPE__PATH:
				setPath((String)newValue);
				return;
			case MulePackage.FILTER_TYPE__PATTERN:
				setPattern((String)newValue);
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
			case MulePackage.FILTER_TYPE__MIXED:
				getMixed().clear();
				return;
			case MulePackage.FILTER_TYPE__PROPERTIES:
				setProperties((PropertiesType)null);
				return;
			case MulePackage.FILTER_TYPE__FILTER:
				setFilter((FilterType)null);
				return;
			case MulePackage.FILTER_TYPE__LEFT_FILTER:
				setLeftFilter((LeftFilterType)null);
				return;
			case MulePackage.FILTER_TYPE__RIGHT_FILTER:
				setRightFilter((RightFilterType)null);
				return;
			case MulePackage.FILTER_TYPE__CLASS_NAME:
				setClassName(CLASS_NAME_EDEFAULT);
				return;
			case MulePackage.FILTER_TYPE__CONFIG_FILE:
				setConfigFile(CONFIG_FILE_EDEFAULT);
				return;
			case MulePackage.FILTER_TYPE__EXPECTED_TYPE:
				setExpectedType(EXPECTED_TYPE_EDEFAULT);
				return;
			case MulePackage.FILTER_TYPE__EXPRESSION:
				setExpression(EXPRESSION_EDEFAULT);
				return;
			case MulePackage.FILTER_TYPE__PATH:
				setPath(PATH_EDEFAULT);
				return;
			case MulePackage.FILTER_TYPE__PATTERN:
				setPattern(PATTERN_EDEFAULT);
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
			case MulePackage.FILTER_TYPE__MIXED:
				return mixed != null && !mixed.isEmpty();
			case MulePackage.FILTER_TYPE__PROPERTIES:
				return getProperties() != null;
			case MulePackage.FILTER_TYPE__FILTER:
				return getFilter() != null;
			case MulePackage.FILTER_TYPE__LEFT_FILTER:
				return getLeftFilter() != null;
			case MulePackage.FILTER_TYPE__RIGHT_FILTER:
				return getRightFilter() != null;
			case MulePackage.FILTER_TYPE__CLASS_NAME:
				return CLASS_NAME_EDEFAULT == null ? className != null : !CLASS_NAME_EDEFAULT.equals(className);
			case MulePackage.FILTER_TYPE__CONFIG_FILE:
				return CONFIG_FILE_EDEFAULT == null ? configFile != null : !CONFIG_FILE_EDEFAULT.equals(configFile);
			case MulePackage.FILTER_TYPE__EXPECTED_TYPE:
				return EXPECTED_TYPE_EDEFAULT == null ? expectedType != null : !EXPECTED_TYPE_EDEFAULT.equals(expectedType);
			case MulePackage.FILTER_TYPE__EXPRESSION:
				return EXPRESSION_EDEFAULT == null ? expression != null : !EXPRESSION_EDEFAULT.equals(expression);
			case MulePackage.FILTER_TYPE__PATH:
				return PATH_EDEFAULT == null ? path != null : !PATH_EDEFAULT.equals(path);
			case MulePackage.FILTER_TYPE__PATTERN:
				return PATTERN_EDEFAULT == null ? pattern != null : !PATTERN_EDEFAULT.equals(pattern);
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
		result.append(", configFile: ");
		result.append(configFile);
		result.append(", expectedType: ");
		result.append(expectedType);
		result.append(", expression: ");
		result.append(expression);
		result.append(", path: ");
		result.append(path);
		result.append(", pattern: ");
		result.append(pattern);
		result.append(')');
		return result.toString();
	}

} //FilterTypeImpl
