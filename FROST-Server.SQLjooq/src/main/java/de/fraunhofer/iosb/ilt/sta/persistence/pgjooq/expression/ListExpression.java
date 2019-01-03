/*
 * Copyright (C) 2016 Fraunhofer Institut IOSB, Fraunhoferstr. 1, D 76131
 * Karlsruhe, Germany.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.fraunhofer.iosb.ilt.sta.persistence.pgjooq.expression;

import java.util.Collection;
import java.util.Map;
import org.jooq.Field;

/**
 * Some paths, like Observation.result and the time-interval paths, return two
 * column references. This class is just to encapsulate these cases.
 */
public class ListExpression implements FieldWrapper {

    private final Map<String, Field> expressions;
    private final Map<String, Field> expressionsForOrder;

    public ListExpression(Map<String, Field> expressions) {
        this.expressions = expressions;
        this.expressionsForOrder = expressions;
    }

    public ListExpression(Map<String, Field> expressions, Map<String, Field> expressionsForOrder) {
        this.expressions = expressions;
        this.expressionsForOrder = expressionsForOrder;
    }

    @Override
    public Field getDefaultField() {
        return expressions.values().iterator().next();
    }

    @Override
    public <T> Field<T> checkType(Class<T> expectedClazz, boolean canCast) {
        Collection<Field> values = expressions.values();
        // Two passes, first do an exact check (no casting allowed)
        for (Field subResult : values) {
            Class fieldType = subResult.getType();
            if (expectedClazz.isAssignableFrom(fieldType)) {
                return subResult;
            }
        }
        // No exact check. Now check again, but allow casting.
        for (Field subResult : values) {
            Class fieldType = subResult.getType();
            if (expectedClazz == String.class && Number.class.isAssignableFrom(fieldType)) {
                return subResult.cast(String.class);
            }
        }
        return null;
    }

    public Map<String, Field> getExpressions() {
        return expressions;
    }

    public Map<String, Field> getExpressionsForOrder() {
        return expressionsForOrder;
    }

    public Field getExpression(String name) {
        return expressions.get(name);
    }

}
