/*
 * Copyright (C) 2022 Joao Assuncao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jassuncao.osgi.cm.sql;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Uses portions of code from Apache Felix Config Admin
 *
 * @author jassuncao
 *
 */
class PropertyConverter {

    protected static final char TOKEN_COMMA = ',';

    // simple types (string & primitive wrappers)
    protected static final char TOKEN_SIMPLE_STRING = 'T';

    protected static final char TOKEN_SIMPLE_INTEGER = 'I';

    protected static final char TOKEN_SIMPLE_LONG = 'L';

    protected static final char TOKEN_SIMPLE_FLOAT = 'F';

    protected static final char TOKEN_SIMPLE_DOUBLE = 'D';

    protected static final char TOKEN_SIMPLE_BYTE = 'X';

    protected static final char TOKEN_SIMPLE_SHORT = 'S';

    protected static final char TOKEN_SIMPLE_CHARACTER = 'C';

    protected static final char TOKEN_SIMPLE_BOOLEAN = 'B';

    protected static final char TOKEN_GENERIC_OBJECT = '?';

    // primitives
    protected static final char TOKEN_PRIMITIVE_INT = 'i';

    protected static final char TOKEN_PRIMITIVE_LONG = 'l';

    protected static final char TOKEN_PRIMITIVE_FLOAT = 'f';

    protected static final char TOKEN_PRIMITIVE_DOUBLE = 'd';

    protected static final char TOKEN_PRIMITIVE_BYTE = 'x';

    protected static final char TOKEN_PRIMITIVE_SHORT = 's';

    protected static final char TOKEN_PRIMITIVE_CHAR = 'c';

    protected static final char TOKEN_PRIMITIVE_BOOLEAN = 'b';

    protected static final Map<Character, Class<?>> code2Type;

    protected static final Map<Class<?>, Character> type2Code;

    static {
        type2Code = new HashMap<>();

        type2Code.put(String.class, TOKEN_SIMPLE_STRING);
        type2Code.put(Integer.class, TOKEN_SIMPLE_INTEGER);
        type2Code.put(Long.class, TOKEN_SIMPLE_LONG);
        type2Code.put(Float.class, TOKEN_SIMPLE_FLOAT);
        type2Code.put(Double.class, TOKEN_SIMPLE_DOUBLE);
        type2Code.put(Byte.class, TOKEN_SIMPLE_BYTE);
        type2Code.put(Short.class, TOKEN_SIMPLE_SHORT);
        type2Code.put(Character.class, TOKEN_SIMPLE_CHARACTER);
        type2Code.put(Boolean.class, TOKEN_SIMPLE_BOOLEAN);
        type2Code.put(Object.class, TOKEN_GENERIC_OBJECT);

        // primitives
        type2Code.put(Integer.TYPE, TOKEN_PRIMITIVE_INT);
        type2Code.put(Long.TYPE, TOKEN_PRIMITIVE_LONG);
        type2Code.put(Float.TYPE, TOKEN_PRIMITIVE_FLOAT);
        type2Code.put(Double.TYPE, TOKEN_PRIMITIVE_DOUBLE);
        type2Code.put(Byte.TYPE, TOKEN_PRIMITIVE_BYTE);
        type2Code.put(Short.TYPE, TOKEN_PRIMITIVE_SHORT);
        type2Code.put(Character.TYPE, TOKEN_PRIMITIVE_CHAR);
        type2Code.put(Boolean.TYPE, TOKEN_PRIMITIVE_BOOLEAN);

        code2Type = new HashMap<>();
        for (Entry<Class<?>, Character> entry : type2Code.entrySet()) {
            code2Type.put(entry.getValue(), entry.getKey());
        }

    }

    private PropertyConverter() {
    }

    /**
     * @param type
     * @param valueAsString
     * @return
     */
    static Object convertFromString(String type, String valueAsString) {
        char code = type.charAt(0);
        if (type.length() >= 3) {
            String suffix = type.substring(1, 3);
            if ("[]".equals(suffix)) {
                return convertStringToArray(code, valueAsString);

            } else if ("()".equals(suffix)) {
                return convertStringToCollection(code, valueAsString);
            }
        }
        return convertStringToSimple(code, valueAsString);
    }

    /**
     * @param code
     * @param valueAsString
     * @return
     */
    private static Object convertStringToSimple(int code, String valueAsString) {
        switch (code) {
            case TOKEN_SIMPLE_STRING:
                return valueAsString;
            // Simple/Primitive, only use wrapper classes
            case TOKEN_SIMPLE_INTEGER:
            case TOKEN_PRIMITIVE_INT:
                return Integer.valueOf(valueAsString);

            case TOKEN_SIMPLE_LONG:
            case TOKEN_PRIMITIVE_LONG:
                return Long.valueOf(valueAsString);

            case TOKEN_SIMPLE_FLOAT:
            case TOKEN_PRIMITIVE_FLOAT:
                int fBits = Integer.parseInt(valueAsString);
                return Float.intBitsToFloat(fBits);

            case TOKEN_SIMPLE_DOUBLE:
            case TOKEN_PRIMITIVE_DOUBLE:
                long dBits = Long.parseLong(valueAsString);
                return Double.longBitsToDouble(dBits);

            case TOKEN_SIMPLE_BYTE:
            case TOKEN_PRIMITIVE_BYTE:
                return Byte.valueOf(valueAsString);

            case TOKEN_SIMPLE_SHORT:
            case TOKEN_PRIMITIVE_SHORT:
                return Short.valueOf(valueAsString);

            case TOKEN_SIMPLE_CHARACTER:
            case TOKEN_PRIMITIVE_CHAR:
                if (valueAsString != null && valueAsString.length() > 0) {
                    return valueAsString.charAt(0);
                }
                return null;

            case TOKEN_SIMPLE_BOOLEAN:
            case TOKEN_PRIMITIVE_BOOLEAN:
                return Boolean.valueOf(valueAsString);

            // unknown type code
            case -1:
            default:
                return null;
        }
    }

    /**
     * @param code
     * @param valueAsString
     * @return
     */
    private static Collection<Object> convertStringToCollection(char code, String valueAsString) {
        Collection<Object> collection = new ArrayList<>();
        List<String> elems = splitElements(valueAsString);
        for (String el : elems) {
            Object obj = convertStringToSimple(code, el);
            if (obj != null) {
                collection.add(obj);
            }
        }
        return collection;
    }

    /**
     * @param code
     * @param valueAsString
     * @return
     */
    private static Object convertStringToArray(char code, String valueAsString) {
        List<String> list = splitElements(valueAsString);
        Class<?> type = code2Type.get(code);
        Object array = Array.newInstance(type, list.size());
        for (int i = 0; i < list.size(); i++) {
            Object obj = convertStringToSimple(code, list.get(i));
            Array.set(array, i, obj);
        }
        return array;
    }

    private static List<String> splitElements(String s) {
        ArrayList<String> elements = new ArrayList<>();
        StringBuilder collector = new StringBuilder();
        boolean underEscape = false;
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (underEscape) {
                collector.append(c);
                underEscape = false;
            } else {
                if (c == '\\') {
                    underEscape = true;
                } else if (c == ',' && collector.length() > 0) {
                    elements.add(collector.toString());
                    collector.setLength(0);
                } else {
                    collector.append(c);
                }
            }
        }
        if (collector.length() > 0) {
            elements.add(collector.toString());
        }
        return elements;
    }

    /**
     * @param value
     * @return
     */
    static String getCodeForType(Object value) {
        Class<?> clazz = value.getClass();
        Class<?> valueType;
        String suffix = "";
        if (clazz.isArray()) {
            valueType = clazz.getComponentType();
            suffix = "[]";
        } else if (value instanceof Collection) {
            suffix = "()";
            Collection<?> collection = (Collection<?>) value;
            if (collection.isEmpty()) {
                valueType = Object.class;
            } else {
                valueType = collection.iterator().next().getClass();
            }
        } else {
            valueType = clazz;
        }
        Character code = type2Code.get(valueType);
        if (code != null) {
            return code + "" + suffix;
        }
        return null;
    }

    static String convertToString(Object value) {
        StringBuilder builder = new StringBuilder();
        Class<?> clazz = value.getClass();
        if (clazz.isArray()) {
            convertArrayToString(builder, value);
        } else if (value instanceof Collection) {
            convertCollectionToString(builder, (Collection<?>) value);
        } else {
            convertSimpleToString(builder, value, false);
        }
        return builder.toString();
    }

    private static void convertArrayToString(StringBuilder builder, Object arrayValue) {
        int size = Array.getLength(arrayValue);

        for (int i = 0; i < size; i++) {
            if (i > 0)
                builder.append(TOKEN_COMMA);
            convertSimpleToString(builder, Array.get(arrayValue, i), true);
        }

    }

    private static void convertCollectionToString(StringBuilder builder, Collection<?> collection) {
        if (!collection.isEmpty()) {
            Iterator<?> ci = collection.iterator();
            Object firstElement = ci.next();

            convertSimpleToString(builder, firstElement, true);
            while (ci.hasNext()) {
                builder.append(TOKEN_COMMA);
                convertSimpleToString(builder, ci.next(), true);
            }
        }
    }

    private static void convertSimpleToString(StringBuilder builder, Object value, boolean needsEscape) {
        Object val;
        if (value instanceof Double) {
            double dVal = (Double) value;
            val = Double.doubleToRawLongBits(dVal);
        } else if (value instanceof Float) {
            float fVal = (Float) value;
            val = Float.floatToRawIntBits(fVal);
        } else {
            val = value;
        }
        String s = String.valueOf(val);
        if (needsEscape) {
            for (int i = 0; i < s.length(); ++i) {
                char c = s.charAt(i);
                if (c == TOKEN_COMMA || c == '\\') {
                    builder.append('\\');
                }
                builder.append(c);
            }
        } else {
            builder.append(s);
        }

    }

}
