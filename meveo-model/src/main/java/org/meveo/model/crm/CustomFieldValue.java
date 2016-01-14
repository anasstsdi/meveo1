package org.meveo.model.crm;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

import org.meveo.commons.utils.StringUtils;
import org.meveo.model.BusinessEntity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Encapsulates a custom field value. Supports the following data types:
 * <ul>
 * <li>string, date, double and long that are stored as separate fields</li>
 * <li>reference to an entity, that is serialized as Json to serializedValue field</li>
 * <li>a list or a map of above mentioned data types, serialized as Json to to serializedValue field</li>
 * </ul>
 * 
 * A reference to an entity, list and map values should not be modified behind the scenes - an appropriate SET method has to be called to serialise the value. - This limitations
 * comes from MERGE loosing transient values and thus JPA callback @postUpdate can not be used (see CustomFieldInstance class).
 * 
 * Serialised value format is the following: <entity/list/map>_<list/map data type>|<value in JSON format>. E.g.
 * 
 * entityReferenceValueForGUI, mapValuesForGUI fields are used in data entry from GUI ONLY.
 * 
 * @author Andrius Karpavicius
 * 
 */
@Embeddable
public class CustomFieldValue implements Serializable {

    private static final long serialVersionUID = -9038541899269528670L;

    public static String MAP_KEY = "key";
    public static String MAP_VALUE = "value";

    public static String MATRIX_COLUMN_NAME_SEPARATOR = "/";
    public static String MATRIX_KEY_SEPARATOR = "|";
    public static String RON_VALUE_SEPARATOR = "<";

    private static String SERIALIZATION_SEPARATOR = "|";

    @Column(name = "STRING_VALUE", columnDefinition = "TEXT")
    private String stringValue;

    @Column(name = "DATE_VALUE")
    private Date dateValue;

    @Column(name = "LONG_VALUE")
    private Long longValue;

    @Column(name = "DOUBLE_VALUE")
    private Double doubleValue;

    @Column(name = "SERIALIZED_VALUE", nullable = true)
    private String serializedValue;

    /**
     * Entity reference type value deserialized from serializedValue field
     */
    @Transient
    private EntityReferenceWrapper entityReferenceValue;

    /**
     * List type value deserialized from serializedValue field
     */
    @Transient
    private List<Object> listValue = null; // new ArrayList<Object>();

    /**
     * Map type value deserialized from serializedValue field
     */
    @Transient
    private Map<String, Object> mapValue = null; // new HashMap<String, Object>();

    /**
     * Contains mapValue adapted for GUI data entry in the following way:
     * 
     * List item corresponds to an entry in a mapValue with the following list's map values: MAP_KEY=mapValue.entry.key and MAP_VALUE=mapValue.entry.value
     */
    @Transient
    private List<Map<String, Object>> mapValuesForGUI = new ArrayList<Map<String, Object>>();

    /**
     * Contains mapValue adapted for GUI data entry in the following way:
     * 
     * List item corresponds to an entry in a mapValue with the following list's map values: MAP_VALUE=mapValue.entry.value mapValue.entry.key is parsed into separate key/value
     * pairs and inserted into map
     */
    @Transient
    private List<Map<String, Object>> matrixValuesForGUI = new ArrayList<Map<String, Object>>();

    /**
     * Contains entityReferenceValue converted into a BusinessEntity object in the following way:
     * 
     * A class of entityReferenceValue.className type is instantiated with code field set to entityReferenceValue.code value
     */
    @Transient
    private BusinessEntity entityReferenceValueForGUI;

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public Date getDateValue() {
        return dateValue;
    }

    public void setDateValue(Date dateValue) {
        this.dateValue = dateValue;
    }

    public Long getLongValue() {
        return longValue;
    }

    public void setLongValue(Long longValue) {
        this.longValue = longValue;
    }

    public Double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public void setMapValuesForGUI(List<Map<String, Object>> mapValuesForGUI) {
        this.mapValuesForGUI = mapValuesForGUI;
    }

    public List<Map<String, Object>> getMapValuesForGUI() {
        return mapValuesForGUI;
    }

    public List<Map<String, Object>> getMatrixValuesForGUI() {
        return matrixValuesForGUI;
    }

    public void setMatrixValuesForGUI(List<Map<String, Object>> matrixValuesForGUI) {
        this.matrixValuesForGUI = matrixValuesForGUI;
    }

    public EntityReferenceWrapper getEntityReferenceValue() {
        return entityReferenceValue;
    }

    /**
     * Set a reference to an entity value. Value is serialised immediately.
     * 
     * NOTE: Always set a new value. DO NOT edit the value, as it will not be persisted.
     * 
     * @param entityReferenceValue Reference to an entity value
     */
    public void setEntityReferenceValue(EntityReferenceWrapper entityReferenceValue) {
        this.entityReferenceValue = entityReferenceValue;
        serializeValue();
    }

    // public String getSerializedValue() {
    // return serializedValue;
    // }

    // public void setSerializedValue(String serializedValue) {
    // this.serializedValue = serializedValue;
    // }

    public List<Object> getListValue() {
        return listValue;
    }

    /**
     * Set a list of values. Value is serialised immediately.
     * 
     * NOTE: Always set a new value. DO NOT edit the value, as it will not be persisted.
     * 
     * @param listValue
     */
    public void setListValue(List<Object> listValue) {
        this.listValue = listValue;
        serializeValue();
    }

    public Map<String, Object> getMapValue() {
        return mapValue;
    }

    /**
     * Set a map of values. Value is serialised immediately.
     * 
     * NOTE: Always set a new value. DO NOT edit the value, as it will not be persisted.
     * 
     * @param mapValue A map of values
     */
    public void setMapValue(Map<String, Object> mapValue) {
        this.mapValue = mapValue;
        serializeValue();
    }

    public void setEntityReferenceValueForGUI(BusinessEntity businessEntity) {
        this.entityReferenceValueForGUI = businessEntity;
    }

    public BusinessEntity getEntityReferenceValueForGUI() {
        return entityReferenceValueForGUI;
    }

    /**
     * Set value of a given type
     * 
     * @param value
     * @param fieldType
     */
    public void setSingleValue(Object value, CustomFieldTypeEnum fieldType) {

        switch (fieldType) {
        case DATE:
            dateValue = (Date) value;
            break;

        case DOUBLE:

            if (value instanceof BigDecimal) {
                doubleValue = ((BigDecimal) value).setScale(2, RoundingMode.HALF_UP).doubleValue();
            } else {
                doubleValue = (Double) value;
            }
            break;

        case LONG:
            if (value instanceof BigDecimal) {
                longValue = ((BigDecimal) value).longValue();
            } else {
                longValue = (Long) value;
            }
            break;

        case STRING:
        case LIST:
        case TEXT_AREA:
            stringValue = (String) value;

            break;
        case ENTITY:
            setEntityReferenceValue(new EntityReferenceWrapper((BusinessEntity) value));
        }
    }

    public String toJson(SimpleDateFormat sdf) {
        String result = "";

        if (stringValue != null) {
            result += "'" + stringValue + "'";
        } else if (dateValue != null) {
            result += "'" + sdf.format(dateValue) + "'";
        } else if (longValue != null) {
            result += longValue;
        } else if (doubleValue != null) {
            result += doubleValue;
        } else {
            result = "";
        }

        return result;
    }

    public String getValueAsString(SimpleDateFormat sdf) {
        String result = "";

        if (stringValue != null) {
            result += stringValue;
        } else if (dateValue != null) {
            result += sdf.format(dateValue);
        } else if (longValue != null) {
            result += longValue;
        } else if (doubleValue != null) {
            result += doubleValue;
        } else {
            result = "";
        }

        return result;
    }

    /**
     * Get a short representation of a value to be used as display in GUI.
     * 
     * @param cft Custom field template
     * @param dateFormat Date format
     * @return Return formated value when storage type is Single and concatenated values when storage type is multiple
     */
    @SuppressWarnings("unchecked")
    public static String getShortRepresentationOfValueObj(Object value, String dateFormat) {
        if (value == null) {
            return null;

        } else if (value instanceof Map) {
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);

            StringBuilder builder = new StringBuilder();
            int i = 0;
            for (Map.Entry<String, Object> valueInfo : ((Map<String, Object>) value).entrySet()) {
                builder.append(builder.length() == 0 ? "" : ", ");
                Object val = valueInfo.getValue();
                if (val instanceof Date) {
                    val = sdf.format(val);
                } else if (val instanceof EntityReferenceWrapper) {
                    val = ((EntityReferenceWrapper) val).getCode();
                }

                builder.append(String.format("%s: [%s]", valueInfo.getKey(), val));
                i++;
                if (i >= 10) {
                    break;
                }
            }

            return builder.toString();

        } else if (value instanceof List) {
            StringBuilder builder = new StringBuilder();
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
            int i = 0;
            for (Object val : ((List<Object>) value)) {
                builder.append(builder.length() == 0 ? "" : ", ");
                if (val instanceof Date) {
                    val = sdf.format(val);
                } else if (val instanceof EntityReferenceWrapper) {
                    val = ((EntityReferenceWrapper) val).getCode();
                }

                builder.append(val);
                i++;
                if (i >= 10) {
                    break;
                }
            }

            return builder.toString();

        } else if (value instanceof EntityReferenceWrapper) {
            return ((EntityReferenceWrapper) value).getCode();

        } else if (value instanceof Date) {
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
            return sdf.format((Date) value);

        } else {
            return value.toString();
        }

    }

    /**
     * Get a short representation of a value to be used as display in GUI.
     * 
     * @param cft Custom field template
     * @param dateFormat Date format
     * @return Return formated value when storage type is Single and concatenated values when storage type is multiple
     */
    public String getShortRepresentationOfValue(CustomFieldTemplate cft, String dateFormat) {

        if (cft.getStorageType() == CustomFieldStorageTypeEnum.LIST || cft.getStorageType() == CustomFieldStorageTypeEnum.MAP) {
            StringBuilder builder = new StringBuilder();

            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
            int i = 0;
            for (Map<String, Object> valueInfo : mapValuesForGUI) {
                builder.append(builder.length() == 0 ? "" : ", ");
                Object value = valueInfo.get(MAP_VALUE);
                if (cft.getFieldType() == CustomFieldTypeEnum.DATE) {

                    value = sdf.format(value);
                } else if (cft.getFieldType() == CustomFieldTypeEnum.ENTITY && value != null) {
                    value = ((BusinessEntity) value).getCode();
                }

                if (cft.getStorageType() == CustomFieldStorageTypeEnum.LIST) {
                    builder.append(value);
                } else {
                    builder.append(String.format("%s: [%s]", valueInfo.get(MAP_KEY), value));
                }
                i++;
                if (i >= 10) {
                    break;
                }
            }

            if (mapValuesForGUI.size() > 10) {
                builder.append(", ...");
            }

            return builder.toString();

        } else if (cft.getStorageType() == CustomFieldStorageTypeEnum.MATRIX) {
            StringBuilder builder = new StringBuilder();

            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
            int i = 0;
            for (Map<String, Object> mapInfo : matrixValuesForGUI) {

                Object value = mapInfo.get(MAP_VALUE);
                if (value == null) {
                    continue;
                }

                if (cft.getFieldType() == CustomFieldTypeEnum.DATE) {
                    value = sdf.format(value);

                } else if (cft.getFieldType() == CustomFieldTypeEnum.ENTITY && value != null) {
                    value = ((BusinessEntity) value).getCode();
                }

                StringBuilder valBuilder = new StringBuilder();

                for (Entry<String, Object> valueInfo : mapInfo.entrySet()) {
                    if (valueInfo.getKey().equals(MAP_VALUE)) {
                        continue;
                    }

                    valBuilder.append(valBuilder.length() == 0 ? "" : "|");
                    valBuilder.append(valueInfo.getKey()).append("/").append(valueInfo.getValue());
                }
                builder.append(builder.length() == 0 ? "" : ", ");
                builder.append(String.format("%s: [%s]", valBuilder.toString(), value));
                i++;
                if (i >= 10) {
                    break;
                }
            }

            if (matrixValuesForGUI.size() > 10) {
                builder.append(", ...");
            }

            return builder.toString();

        } else if (cft.getStorageType() == CustomFieldStorageTypeEnum.SINGLE) {
            switch (cft.getFieldType()) {
            case DATE:
                if (dateValue != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
                    return sdf.format(dateValue);
                }
                break;
            case DOUBLE:
                if (doubleValue != null) {
                    return doubleValue.toString();
                }
                break;
            case ENTITY:
                if (entityReferenceValue != null) {
                    return entityReferenceValue.getCode();
                }
                break;
            case LONG:
                if (longValue != null) {
                    return longValue.toString();
                }
                break;
            case STRING:
            case LIST:
            case TEXT_AREA:
                return stringValue;
            }
        }
        return null;
    }

    /**
     * Check if values is empty when used in data entry/display for GUI (use XXXForGUI fields instead of serializedValue field )
     * 
     * @return True is value is empty
     */
    protected boolean isValueEmptyForGui() {
        boolean isEmpty = ((stringValue == null || stringValue.isEmpty()) && dateValue == null && longValue == null && doubleValue == null && entityReferenceValueForGUI == null
                && (mapValuesForGUI == null || mapValuesForGUI.isEmpty()) && (matrixValuesForGUI == null || matrixValuesForGUI.isEmpty()));

        if (isEmpty) {
            return true;

        } else if (matrixValuesForGUI != null && !matrixValuesForGUI.isEmpty()) {
            for (Map<String, Object> mapValue : matrixValuesForGUI) {
                for (Object value : mapValue.values()) {
                    boolean empty = StringUtils.isBlank(value);
                    if (!empty) {
                        return false;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check if values is empty when used in non-GUI data manipulation (use serializedValue instead of XXXForGUI fields)
     * 
     * @return True is value is empty
     */
    protected boolean isValueEmpty() {
        return ((stringValue == null || stringValue.isEmpty()) && dateValue == null && longValue == null && doubleValue == null && (serializedValue == null || serializedValue
            .isEmpty()));
    }

    /**
     * Serialise a reference to an entity, list or map of values to a Json string, stored in serializedValue field in the following format:
     * <ul>
     * <li>"entity"|<json representation of EntityReferenceWrapper></li>
     * <li>"list_"<value classname eg. String>|<json representation of List></li>
     * <li>"map_"<value classname eg. String>|<json representation of Map></li>
     * <li>"matrix_"<value classname eg. String>|<key names>|<json representation of Map></li>
     * </ul>
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void serializeValue() {

        GsonBuilder builder = new GsonBuilder().setDateFormat("yyyy-dd-MM HH:mm:ss zzz");
        Gson gson = builder.create();

        String sValue = null;
        if (entityReferenceValue != null && !entityReferenceValue.isEmpty()) {
            sValue = "entity"+SERIALIZATION_SEPARATOR + gson.toJson(entityReferenceValue);

        } else if (listValue != null && !listValue.isEmpty()) {
            Class itemClass = listValue.get(0).getClass();
            sValue = "list_" + itemClass.getSimpleName() + SERIALIZATION_SEPARATOR + gson.toJson(listValue);

        } else if (mapValue != null && !mapValue.isEmpty()) {

            // Handle map that stores matrix type values
            if (mapValue.containsKey(MAP_KEY)) {

                Map<String, Object> mapCopy = new HashMap<String, Object>();
                mapCopy.putAll(mapValue);
                mapCopy.remove(MAP_KEY);

                Object columnNames = mapValue.get(MAP_KEY);
                String columnNamesString = null;
                if (columnNames instanceof String) {
                    columnNamesString = (String) columnNames;

                } else if (columnNames instanceof Collection) {
                    columnNamesString = StringUtils.concatenate(MATRIX_COLUMN_NAME_SEPARATOR, (Collection) columnNames);
                }

                Class itemClass = mapValue.values().iterator().next().getClass();
                sValue = "matrix_" + itemClass.getSimpleName() + SERIALIZATION_SEPARATOR + columnNamesString + SERIALIZATION_SEPARATOR + gson.toJson(mapCopy);

                // A regular map
            } else {
                Class itemClass = mapValue.values().iterator().next().getClass();
                sValue = "map_" + itemClass.getSimpleName() + SERIALIZATION_SEPARATOR + gson.toJson(mapValue);
            }
        }
        serializedValue = sValue;

    }

    /**
     * Deserialize serializedValue field to a reference to an entity, list or map of values. See method serialize() for serialized value format
     */
    public void deserializeValue() {
        if (serializedValue == null) {
            return;
        }

        GsonBuilder builder = new GsonBuilder().setDateFormat("yyyy-dd-MM HH:mm:ss zzz");
        Gson gson = builder.create();
        
        int firstSeparatorIndex = serializedValue.indexOf(SERIALIZATION_SEPARATOR);

        String type = serializedValue.substring(0, firstSeparatorIndex);
        String subType = null;
        if (type.indexOf('_') > 0) {
            subType = type.substring(serializedValue.indexOf("_") + 1);
            type = type.substring(0, serializedValue.indexOf("_"));
        }

        if ("entity".equals(type)) {
            String sValue = serializedValue.substring(firstSeparatorIndex + 1);
            entityReferenceValue = gson.fromJson(sValue, EntityReferenceWrapper.class);

        } else if ("list".equals(type)) {

            // Type defaults to String
            Type itemType = new TypeToken<List<String>>() {
            }.getType();

            // Determine an appropriate type
            if (Date.class.getSimpleName().equals(subType)) {
                itemType = new TypeToken<List<Date>>() {
                }.getType();
            } else if (Double.class.getSimpleName().equals(subType)) {
                itemType = new TypeToken<List<Double>>() {
                }.getType();
            } else if (Long.class.getSimpleName().equals(subType)) {
                itemType = new TypeToken<List<Long>>() {
                }.getType();
            } else if (EntityReferenceWrapper.class.getSimpleName().equals(subType)) {
                itemType = new TypeToken<List<EntityReferenceWrapper>>() {
                }.getType();
            }

            String sValue = serializedValue.substring(firstSeparatorIndex + 1);
            listValue = gson.fromJson(sValue, itemType);

        } else if ("map".equals(type)) {

            // Type defaults to String
            Type itemType = new TypeToken<Map<String, String>>() {
            }.getType();

            // Determine an appropriate type
            if (Date.class.getSimpleName().equals(subType)) {
                itemType = new TypeToken<Map<String, Date>>() {
                }.getType();
            } else if (Double.class.getSimpleName().equals(subType)) {
                itemType = new TypeToken<Map<String, Double>>() {
                }.getType();
            } else if (Long.class.getSimpleName().equals(subType)) {
                itemType = new TypeToken<Map<String, Long>>() {
                }.getType();
            } else if (EntityReferenceWrapper.class.getSimpleName().equals(subType)) {
                itemType = new TypeToken<Map<String, EntityReferenceWrapper>>() {
                }.getType();
            }

            String sValue = serializedValue.substring(firstSeparatorIndex + 1);
            mapValue = gson.fromJson(sValue, itemType);

        } else if ("matrix".equals(type)) {

            // Type defaults to String
            Type itemType = new TypeToken<Map<String, String>>() {
            }.getType();

            // Determine an appropriate type
            if (Date.class.getSimpleName().equals(subType)) {
                itemType = new TypeToken<Map<String, Date>>() {
                }.getType();
            } else if (Double.class.getSimpleName().equals(subType)) {
                itemType = new TypeToken<Map<String, Double>>() {
                }.getType();
            } else if (Long.class.getSimpleName().equals(subType)) {
                itemType = new TypeToken<Map<String, Long>>() {
                }.getType();
            } else if (EntityReferenceWrapper.class.getSimpleName().equals(subType)) {
                itemType = new TypeToken<Map<String, EntityReferenceWrapper>>() {
                }.getType();
            }

            
            int secondSeparatorIndex = serializedValue.indexOf(SERIALIZATION_SEPARATOR, firstSeparatorIndex + 1);
            String keys = serializedValue.substring(firstSeparatorIndex + 1, secondSeparatorIndex);
            String sValue = serializedValue.substring(secondSeparatorIndex + 1);

            mapValue = gson.fromJson(sValue, itemType);
            mapValue.put(MAP_KEY, keys);
        }
    }

    public Object getValue() {
        if (mapValue != null && !mapValue.isEmpty()) {
            return mapValue;
        } else if (listValue != null && !listValue.isEmpty()) {
            return listValue;
        } else if (stringValue != null) {
            return stringValue;
        } else if (dateValue != null) {
            return dateValue;
        } else if (doubleValue != null) {
            return doubleValue;
        } else if (longValue != null) {
            return longValue;
        } else if (entityReferenceValue != null) {
            return entityReferenceValue;
        }
        return null;
    }

    /**
     * Set value of a given type
     * 
     * @param value
     * @param fieldType
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void setValue(Object value) {

        if (value instanceof Date) {
            dateValue = (Date) value;

        } else if (value instanceof BigDecimal) {
            doubleValue = ((BigDecimal) value).setScale(2, RoundingMode.HALF_UP).doubleValue();

        } else if (value instanceof Double) {
            doubleValue = (Double) value;

        } else if (value instanceof Long) {
            longValue = (Long) value;

        } else if (value instanceof String) {
            stringValue = (String) value;

        } else if (value instanceof BusinessEntity) {
            setEntityReferenceValue(new EntityReferenceWrapper((BusinessEntity) value));

        } else if (value instanceof EntityReferenceWrapper) {
            setEntityReferenceValue((EntityReferenceWrapper) value);

        } else if (value instanceof Map) {
            setMapValue((Map) value);

        } else if (value instanceof List) {
            setListValue((List) value);
        }
    }

    @Override
    public String toString() {
        final int maxLen = 10;
        return String
            .format(
                "CustomFieldValue [stringValue=%s, dateValue=%s, longValue=%s, doubleValue=%s, serializedValue=%s, entityReferenceValue=%s, listValue=%s, mapValue=%s, mapValuesForGUI=%s, matrixValuesForGUI=%s, entityReferenceValueForGUI=%s]",
                stringValue, dateValue, longValue, doubleValue, serializedValue, entityReferenceValue, listValue != null ? toString(listValue, maxLen) : null,
                mapValue != null ? toString(mapValue.entrySet(), maxLen) : null, mapValuesForGUI != null ? toString(mapValuesForGUI, maxLen) : null,
                matrixValuesForGUI != null ? toString(matrixValuesForGUI, maxLen) : null, entityReferenceValueForGUI);
    }

    private String toString(Collection<?> collection, int maxLen) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        int i = 0;
        for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {
            if (i > 0)
                builder.append(", ");
            builder.append(iterator.next());
        }
        builder.append("]");
        return builder.toString();
    }
}