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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

/**
 * @author jassuncao
 *
 */
public class PropertyConverterTest {

    private static final Integer[] INT_ARRAY = new Integer[] { 1, 2 };

    private static final String INT_ARRAY_TEXT = "1,2";

    private static final String[] STRING_ARRAY = new String[] { "test,1", "test 2" };

    private static final String STRING_ARRAY_TEXT = "test\\,1,test 2";

    @Test
    public void testConvertToString() {
        assertEquals("1", PropertyConverter.convertToString(1));
        assertEquals("true", PropertyConverter.convertToString(Boolean.TRUE));
        assertEquals("1", PropertyConverter.convertToString((short) 1));
        assertEquals("xpto", PropertyConverter.convertToString("xpto"));
        assertEquals(INT_ARRAY_TEXT, PropertyConverter.convertToString(INT_ARRAY));
        assertEquals(STRING_ARRAY_TEXT, PropertyConverter.convertToString(STRING_ARRAY));
        assertEquals(INT_ARRAY_TEXT, PropertyConverter.convertToString(Arrays.asList(INT_ARRAY)));
    }

    @Test
    public void testConvertFromString() {
        assertEquals(1,
                convertFromString(PropertyConverter.TOKEN_SIMPLE_INTEGER, "1"));
        assertEquals("xpto", convertFromString(PropertyConverter.TOKEN_SIMPLE_STRING, "xpto"));
        assertEquals(Boolean.TRUE, convertFromString(PropertyConverter.TOKEN_PRIMITIVE_BOOLEAN, "true"));

        assertArrayEquals(INT_ARRAY, convertFromStringToArray(PropertyConverter.TOKEN_SIMPLE_INTEGER, INT_ARRAY_TEXT));
        assertArrayEquals(STRING_ARRAY,
                convertFromStringToArray(PropertyConverter.TOKEN_SIMPLE_STRING, STRING_ARRAY_TEXT));

        Collection<?> collection = convertFromStringToCollection(PropertyConverter.TOKEN_SIMPLE_INTEGER,
                INT_ARRAY_TEXT);
        assertArrayEquals(INT_ARRAY, collection.toArray());

    }

    private static Object convertFromString(char type, String string) {
        return PropertyConverter.convertFromString(Character.toString(type), string);
    }

    private static Object[] convertFromStringToArray(char type, String string) {
        return (Object[]) PropertyConverter.convertFromString(type + "[]", string);
    }

    private static Collection<?> convertFromStringToCollection(char type, String string) {
        return (Collection<?>) PropertyConverter.convertFromString(type + "()", string);
    }

}
