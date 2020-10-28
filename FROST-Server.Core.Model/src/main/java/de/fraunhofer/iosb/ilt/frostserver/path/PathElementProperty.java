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
package de.fraunhofer.iosb.ilt.frostserver.path;

import de.fraunhofer.iosb.ilt.frostserver.property.EntityPropertyMain;
import java.util.Objects;

/**
 *
 * @author jab
 */
public class PathElementProperty implements PathElement {

    private EntityPropertyMain property;
    private PathElement parent;

    public PathElementProperty() {
    }

    public PathElementProperty(EntityPropertyMain property, PathElement parent) {
        this.property = property;
        this.parent = parent;
    }

    public EntityPropertyMain getProperty() {
        return property;
    }

    @Override
    public PathElement getParent() {
        return parent;
    }

    public void setProperty(EntityPropertyMain property) {
        this.property = property;
    }

    @Override
    public void setParent(PathElement parent) {
        this.parent = parent;
    }

    @Override
    public void visit(ResourcePathVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return property.name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(property, parent);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PathElementProperty other = (PathElementProperty) obj;
        return Objects.equals(this.property, other.property)
                && Objects.equals(this.parent, other.parent);
    }

}
