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

import org.mule.schema.ConstraintType;
import org.mule.schema.FilterType;
import org.mule.schema.LeftFilterType;
import org.mule.schema.MulePackage;
import org.mule.schema.RightFilterType;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Constraint Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.mule.schema.impl.ConstraintTypeImpl#getMixed <em>Mixed</em>}</li>
 *   <li>{@link org.mule.schema.impl.ConstraintTypeImpl#getLeftFilter <em>Left Filter</em>}</li>
 *   <li>{@link org.mule.schema.impl.ConstraintTypeImpl#getRightFilter <em>Right Filter</em>}</li>
 *   <li>{@link org.mule.schema.impl.ConstraintTypeImpl#getFilter <em>Filter</em>}</li>
 *   <li>{@link org.mule.schema.impl.ConstraintTypeImpl#getBatchSize <em>Batch Size</em>}</li>
 *   <li>{@link org.mule.schema.impl.ConstraintTypeImpl#getClassName <em>Class Name</em>}</li>
 *   <li>{@link org.mule.schema.impl.ConstraintTypeImpl#getExpectedType <em>Expected Type</em>}</li>
 *   <li>{@link org.mule.schema.impl.ConstraintTypeImpl#getExpression <em>Expression</em>}</li>
 *   <li>{@link org.mule.schema.impl.ConstraintTypeImpl#getFrequency <em>Frequency</em>}</li>
 *   <li>{@link org.mule.schema.impl.ConstraintTypeImpl#getPath <em>Path</em>}</li>
 *   <li>{@link org.mule.schema.impl.ConstraintTypeImpl#getPattern <em>Pattern</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ConstraintTypeImpl extends EObjectImpl implements ConstraintType {
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
     * The default value of the '{@link #getBatchSize() <em>Batch Size</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getBatchSize()
     * @generated
     * @ordered
     */
    protected static final String BATCH_SIZE_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getBatchSize() <em>Batch Size</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getBatchSize()
     * @generated
     * @ordered
     */
    protected String batchSize = BATCH_SIZE_EDEFAULT;

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
     * The default value of the '{@link #getFrequency() <em>Frequency</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getFrequency()
     * @generated
     * @ordered
     */
    protected static final String FREQUENCY_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getFrequency() <em>Frequency</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getFrequency()
     * @generated
     * @ordered
     */
    protected String frequency = FREQUENCY_EDEFAULT;

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
    protected ConstraintTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    protected EClass eStaticClass() {
        return MulePackage.eINSTANCE.getConstraintType();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public FeatureMap getMixed() {
        if (mixed == null) {
            mixed = new BasicFeatureMap(this, MulePackage.CONSTRAINT_TYPE__MIXED);
        }
        return mixed;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public LeftFilterType getLeftFilter() {
        return (LeftFilterType)getMixed().get(MulePackage.eINSTANCE.getConstraintType_LeftFilter(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public NotificationChain basicSetLeftFilter(LeftFilterType newLeftFilter, NotificationChain msgs) {
        return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getConstraintType_LeftFilter(), newLeftFilter, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public void setLeftFilter(LeftFilterType newLeftFilter) {
        ((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getConstraintType_LeftFilter(), newLeftFilter);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public RightFilterType getRightFilter() {
        return (RightFilterType)getMixed().get(MulePackage.eINSTANCE.getConstraintType_RightFilter(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public NotificationChain basicSetRightFilter(RightFilterType newRightFilter, NotificationChain msgs) {
        return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getConstraintType_RightFilter(), newRightFilter, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public void setRightFilter(RightFilterType newRightFilter) {
        ((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getConstraintType_RightFilter(), newRightFilter);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public FilterType getFilter() {
        return (FilterType)getMixed().get(MulePackage.eINSTANCE.getConstraintType_Filter(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public NotificationChain basicSetFilter(FilterType newFilter, NotificationChain msgs) {
        return ((FeatureMap.Internal)getMixed()).basicAdd(MulePackage.eINSTANCE.getConstraintType_Filter(), newFilter, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public void setFilter(FilterType newFilter) {
        ((FeatureMap.Internal)getMixed()).set(MulePackage.eINSTANCE.getConstraintType_Filter(), newFilter);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public String getBatchSize() {
        return batchSize;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public void setBatchSize(String newBatchSize) {
        String oldBatchSize = batchSize;
        batchSize = newBatchSize;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.CONSTRAINT_TYPE__BATCH_SIZE, oldBatchSize, batchSize));
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
            eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.CONSTRAINT_TYPE__CLASS_NAME, oldClassName, className));
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
            eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.CONSTRAINT_TYPE__EXPECTED_TYPE, oldExpectedType, expectedType));
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
            eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.CONSTRAINT_TYPE__EXPRESSION, oldExpression, expression));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public String getFrequency() {
        return frequency;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public void setFrequency(String newFrequency) {
        String oldFrequency = frequency;
        frequency = newFrequency;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.CONSTRAINT_TYPE__FREQUENCY, oldFrequency, frequency));
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
            eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.CONSTRAINT_TYPE__PATH, oldPath, path));
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
            eNotify(new ENotificationImpl(this, Notification.SET, MulePackage.CONSTRAINT_TYPE__PATTERN, oldPattern, pattern));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, Class baseClass, NotificationChain msgs) {
        if (featureID >= 0) {
            switch (eDerivedStructuralFeatureID(featureID, baseClass)) {
                case MulePackage.CONSTRAINT_TYPE__MIXED:
                    return ((InternalEList)getMixed()).basicRemove(otherEnd, msgs);
                case MulePackage.CONSTRAINT_TYPE__LEFT_FILTER:
                    return basicSetLeftFilter(null, msgs);
                case MulePackage.CONSTRAINT_TYPE__RIGHT_FILTER:
                    return basicSetRightFilter(null, msgs);
                case MulePackage.CONSTRAINT_TYPE__FILTER:
                    return basicSetFilter(null, msgs);
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
            case MulePackage.CONSTRAINT_TYPE__MIXED:
                return getMixed();
            case MulePackage.CONSTRAINT_TYPE__LEFT_FILTER:
                return getLeftFilter();
            case MulePackage.CONSTRAINT_TYPE__RIGHT_FILTER:
                return getRightFilter();
            case MulePackage.CONSTRAINT_TYPE__FILTER:
                return getFilter();
            case MulePackage.CONSTRAINT_TYPE__BATCH_SIZE:
                return getBatchSize();
            case MulePackage.CONSTRAINT_TYPE__CLASS_NAME:
                return getClassName();
            case MulePackage.CONSTRAINT_TYPE__EXPECTED_TYPE:
                return getExpectedType();
            case MulePackage.CONSTRAINT_TYPE__EXPRESSION:
                return getExpression();
            case MulePackage.CONSTRAINT_TYPE__FREQUENCY:
                return getFrequency();
            case MulePackage.CONSTRAINT_TYPE__PATH:
                return getPath();
            case MulePackage.CONSTRAINT_TYPE__PATTERN:
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
            case MulePackage.CONSTRAINT_TYPE__MIXED:
                getMixed().clear();
                getMixed().addAll((Collection)newValue);
                return;
            case MulePackage.CONSTRAINT_TYPE__LEFT_FILTER:
                setLeftFilter((LeftFilterType)newValue);
                return;
            case MulePackage.CONSTRAINT_TYPE__RIGHT_FILTER:
                setRightFilter((RightFilterType)newValue);
                return;
            case MulePackage.CONSTRAINT_TYPE__FILTER:
                setFilter((FilterType)newValue);
                return;
            case MulePackage.CONSTRAINT_TYPE__BATCH_SIZE:
                setBatchSize((String)newValue);
                return;
            case MulePackage.CONSTRAINT_TYPE__CLASS_NAME:
                setClassName((String)newValue);
                return;
            case MulePackage.CONSTRAINT_TYPE__EXPECTED_TYPE:
                setExpectedType((String)newValue);
                return;
            case MulePackage.CONSTRAINT_TYPE__EXPRESSION:
                setExpression((String)newValue);
                return;
            case MulePackage.CONSTRAINT_TYPE__FREQUENCY:
                setFrequency((String)newValue);
                return;
            case MulePackage.CONSTRAINT_TYPE__PATH:
                setPath((String)newValue);
                return;
            case MulePackage.CONSTRAINT_TYPE__PATTERN:
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
            case MulePackage.CONSTRAINT_TYPE__MIXED:
                getMixed().clear();
                return;
            case MulePackage.CONSTRAINT_TYPE__LEFT_FILTER:
                setLeftFilter((LeftFilterType)null);
                return;
            case MulePackage.CONSTRAINT_TYPE__RIGHT_FILTER:
                setRightFilter((RightFilterType)null);
                return;
            case MulePackage.CONSTRAINT_TYPE__FILTER:
                setFilter((FilterType)null);
                return;
            case MulePackage.CONSTRAINT_TYPE__BATCH_SIZE:
                setBatchSize(BATCH_SIZE_EDEFAULT);
                return;
            case MulePackage.CONSTRAINT_TYPE__CLASS_NAME:
                setClassName(CLASS_NAME_EDEFAULT);
                return;
            case MulePackage.CONSTRAINT_TYPE__EXPECTED_TYPE:
                setExpectedType(EXPECTED_TYPE_EDEFAULT);
                return;
            case MulePackage.CONSTRAINT_TYPE__EXPRESSION:
                setExpression(EXPRESSION_EDEFAULT);
                return;
            case MulePackage.CONSTRAINT_TYPE__FREQUENCY:
                setFrequency(FREQUENCY_EDEFAULT);
                return;
            case MulePackage.CONSTRAINT_TYPE__PATH:
                setPath(PATH_EDEFAULT);
                return;
            case MulePackage.CONSTRAINT_TYPE__PATTERN:
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
            case MulePackage.CONSTRAINT_TYPE__MIXED:
                return mixed != null && !mixed.isEmpty();
            case MulePackage.CONSTRAINT_TYPE__LEFT_FILTER:
                return getLeftFilter() != null;
            case MulePackage.CONSTRAINT_TYPE__RIGHT_FILTER:
                return getRightFilter() != null;
            case MulePackage.CONSTRAINT_TYPE__FILTER:
                return getFilter() != null;
            case MulePackage.CONSTRAINT_TYPE__BATCH_SIZE:
                return BATCH_SIZE_EDEFAULT == null ? batchSize != null : !BATCH_SIZE_EDEFAULT.equals(batchSize);
            case MulePackage.CONSTRAINT_TYPE__CLASS_NAME:
                return CLASS_NAME_EDEFAULT == null ? className != null : !CLASS_NAME_EDEFAULT.equals(className);
            case MulePackage.CONSTRAINT_TYPE__EXPECTED_TYPE:
                return EXPECTED_TYPE_EDEFAULT == null ? expectedType != null : !EXPECTED_TYPE_EDEFAULT.equals(expectedType);
            case MulePackage.CONSTRAINT_TYPE__EXPRESSION:
                return EXPRESSION_EDEFAULT == null ? expression != null : !EXPRESSION_EDEFAULT.equals(expression);
            case MulePackage.CONSTRAINT_TYPE__FREQUENCY:
                return FREQUENCY_EDEFAULT == null ? frequency != null : !FREQUENCY_EDEFAULT.equals(frequency);
            case MulePackage.CONSTRAINT_TYPE__PATH:
                return PATH_EDEFAULT == null ? path != null : !PATH_EDEFAULT.equals(path);
            case MulePackage.CONSTRAINT_TYPE__PATTERN:
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
        result.append(", batchSize: ");
        result.append(batchSize);
        result.append(", className: ");
        result.append(className);
        result.append(", expectedType: ");
        result.append(expectedType);
        result.append(", expression: ");
        result.append(expression);
        result.append(", frequency: ");
        result.append(frequency);
        result.append(", path: ");
        result.append(path);
        result.append(", pattern: ");
        result.append(pattern);
        result.append(')');
        return result.toString();
    }

} //ConstraintTypeImpl
