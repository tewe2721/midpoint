/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json;

import com.evolveum.midpoint.prism.marshaller.XNodeProcessorEvaluationMode;
import com.evolveum.midpoint.prism.util.JavaTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.xnode.ValueParser;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.Map;

public class JsonNullValueParser<T> implements ValueParser<T> {

    public JsonNullValueParser() {
    }

    @Override
    public T parse(QName typeName, XNodeProcessorEvaluationMode mode) throws SchemaException {
        Class clazz = XsdTypeMapper.toJavaType(typeName);
        if (clazz == null) {
            throw new SchemaException("Unsupported type " + typeName);
        }
        return (T) JavaTypeConverter.convert(clazz, "");
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public String getStringValue() {
        return "";
    }

    @Override
    public String toString() {
        return "JsonNullValueParser";
    }

    @Override
    public Map<String, String> getPotentiallyRelevantNamespaces() {
        return null;                // TODO implement
    }

    Element asDomElement() throws IOException {
        return null;
    }
}
