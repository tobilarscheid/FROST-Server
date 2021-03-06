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
package de.fraunhofer.iosb.ilt.frostserver.model;

import com.fasterxml.jackson.core.type.TypeReference;
import de.fraunhofer.iosb.ilt.frostserver.model.core.AbstractDatastream;
import de.fraunhofer.iosb.ilt.frostserver.model.core.EntitySet;
import de.fraunhofer.iosb.ilt.frostserver.model.core.EntitySetImpl;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Id;
import de.fraunhofer.iosb.ilt.frostserver.model.ext.ObservationType;
import de.fraunhofer.iosb.ilt.frostserver.model.ext.UnitOfMeasurement;
import de.fraunhofer.iosb.ilt.frostserver.property.EntityPropertyMain;
import de.fraunhofer.iosb.ilt.frostserver.property.NavigationPropertyMain;
import de.fraunhofer.iosb.ilt.frostserver.util.exception.IncompleteEntityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author jab, scf
 */
public class MultiDatastream extends AbstractDatastream<MultiDatastream> {

    public static final TypeReference<MultiDatastream> TYPE_REFERENCE_MULTIDATASTREAM = new TypeReference<MultiDatastream>() {
        // Empty on purpose.
    };

    private List<String> multiObservationDataTypes;
    private List<UnitOfMeasurement> unitOfMeasurements;
    private EntitySet<ObservedProperty> observedProperties;

    private boolean setMultiObservationDataTypes;
    private boolean setUnitOfMeasurements;

    public MultiDatastream() {
        this(null);
    }

    public MultiDatastream(Id id) {
        super(id);
        observedProperties = new EntitySetImpl<>(EntityType.OBSERVEDPROPERTY);
        unitOfMeasurements = new ArrayList<>();
        multiObservationDataTypes = new ArrayList<>();
        setObservationTypeIntern(ObservationType.COMPLEX_OBSERVATION.getCode());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.MULTIDATASTREAM;
    }

    @Override
    public void complete(boolean entityPropertiesOnly) throws IncompleteEntityException {
        if (unitOfMeasurements.size() != multiObservationDataTypes.size()) {
            throw new IllegalStateException("Size of list of unitOfMeasurements (" + unitOfMeasurements.size() + ") is not equal to size of multiObservationDataTypes (" + multiObservationDataTypes.size() + ").");
        }
        if (!entityPropertiesOnly && observedProperties.size() != multiObservationDataTypes.size()) {
            throw new IllegalStateException("Size of list of observedProperties (" + observedProperties.size() + ") is not equal to size of multiObservationDataTypes (" + multiObservationDataTypes.size() + ").");
        }
        String observationType = getObservationType();
        if (observationType == null || !observationType.equalsIgnoreCase("http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_ComplexObservation")) {
            throw new IllegalStateException("ObservationType must be http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_ComplexObservation.");
        }
        super.complete(entityPropertiesOnly);
    }

    @Override
    public void setEntityPropertiesSet(boolean set, boolean entityPropertiesOnly) {
        super.setEntityPropertiesSet(set, entityPropertiesOnly);
        setSets(set);
    }

    private void setSets(boolean set) {
        setUnitOfMeasurements = set;
        setMultiObservationDataTypes = set;
    }

    @Override
    public void setEntityPropertiesSet(MultiDatastream comparedTo, EntityChangedMessage message) {
        super.setEntityPropertiesSet(comparedTo, message);
        setSets(false);
        if (!Objects.equals(unitOfMeasurements, comparedTo.getUnitOfMeasurements())) {
            setUnitOfMeasurements = true;
            message.addEpField(EntityPropertyMain.UNITOFMEASUREMENTS);
        }
        if (!Objects.equals(multiObservationDataTypes, comparedTo.getMultiObservationDataTypes())) {
            setMultiObservationDataTypes = true;
            message.addEpField(EntityPropertyMain.MULTIOBSERVATIONDATATYPES);
        }
        if (!Objects.equals(observedProperties, comparedTo.getObservedProperties())) {
            message.addNpField(NavigationPropertyMain.OBSERVEDPROPERTIES);
        }
    }

    public List<String> getMultiObservationDataTypes() {
        return multiObservationDataTypes;
    }

    /**
     * @param observationTypes the observationTypes to set
     * @return this
     */
    public MultiDatastream setMultiObservationDataTypes(List<String> observationTypes) {
        this.multiObservationDataTypes = observationTypes;
        setMultiObservationDataTypes = multiObservationDataTypes != null;
        return this;
    }

    public MultiDatastream addObservationType(String observationType) {
        if (multiObservationDataTypes == null) {
            multiObservationDataTypes = new ArrayList<>();
        }
        multiObservationDataTypes.add(observationType);
        return this;
    }

    /**
     * @return the setMultiObservationDataTypes
     */
    public boolean isSetMultiObservationDataTypes() {
        return setMultiObservationDataTypes;
    }

    /**
     * @return the unitOfMeasurements
     */
    public List<UnitOfMeasurement> getUnitOfMeasurements() {
        return unitOfMeasurements;
    }

    /**
     * @param unitsOfMeasurement the unitsOfMeasurement to set
     * @return this
     */
    public MultiDatastream setUnitOfMeasurements(List<UnitOfMeasurement> unitsOfMeasurement) {
        this.unitOfMeasurements = unitsOfMeasurement;
        setUnitOfMeasurements = unitOfMeasurements != null;
        return this;
    }

    public MultiDatastream addUnitOfMeasurement(UnitOfMeasurement unitOfMeasurement) {
        if (unitOfMeasurements == null) {
            unitOfMeasurements = new ArrayList<>();
        }
        unitOfMeasurements.add(unitOfMeasurement);
        return this;
    }

    /**
     * @return the setUnitOfMeasurements
     */
    public boolean isSetUnitOfMeasurements() {
        return setUnitOfMeasurements;
    }

    /**
     * @return the observedProperty
     */
    public EntitySet<ObservedProperty> getObservedProperties() {
        return observedProperties;
    }

    /**
     * @param observedProperties the observedProperty to set
     * @return this
     */
    public MultiDatastream setObservedProperties(EntitySet<ObservedProperty> observedProperties) {
        this.observedProperties = observedProperties;
        return this;
    }

    public MultiDatastream addObservedProperty(ObservedProperty observedProperty) {
        observedProperties.add(observedProperty);
        return this;
    }

    @Override
    protected MultiDatastream getThis() {
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), multiObservationDataTypes, unitOfMeasurements, observedProperties);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MultiDatastream other = (MultiDatastream) obj;
        return super.equals(other)
                && Objects.equals(multiObservationDataTypes, other.multiObservationDataTypes)
                && Objects.equals(observedProperties, other.observedProperties)
                && Objects.equals(unitOfMeasurements, other.unitOfMeasurements);
    }

}
