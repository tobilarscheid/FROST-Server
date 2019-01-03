/*
 * Copyright (C) 2018 Fraunhofer Institut IOSB, Fraunhoferstr. 1, D 76131
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
package de.fraunhofer.iosb.ilt.sta.persistence.pgjooq.factories;

import de.fraunhofer.iosb.ilt.sta.messagebus.EntityChangedMessage;
import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.Observation;
import de.fraunhofer.iosb.ilt.sta.model.ObservedProperty;
import de.fraunhofer.iosb.ilt.sta.model.Sensor;
import de.fraunhofer.iosb.ilt.sta.model.Thing;
import de.fraunhofer.iosb.ilt.sta.model.ext.UnitOfMeasurement;
import de.fraunhofer.iosb.ilt.sta.path.EntityProperty;
import de.fraunhofer.iosb.ilt.sta.path.EntityType;
import de.fraunhofer.iosb.ilt.sta.path.NavigationProperty;
import de.fraunhofer.iosb.ilt.sta.path.Property;
import de.fraunhofer.iosb.ilt.sta.persistence.pgjooq.DataSize;
import de.fraunhofer.iosb.ilt.sta.persistence.pgjooq.EntityFactories;
import static de.fraunhofer.iosb.ilt.sta.persistence.pgjooq.EntityFactories.CAN_NOT_BE_NULL;
import static de.fraunhofer.iosb.ilt.sta.persistence.pgjooq.EntityFactories.CHANGED_MULTIPLE_ROWS;
import static de.fraunhofer.iosb.ilt.sta.persistence.pgjooq.EntityFactories.NO_ID_OR_NOT_FOUND;
import de.fraunhofer.iosb.ilt.sta.persistence.pgjooq.PostgresPersistenceManager;
import de.fraunhofer.iosb.ilt.sta.persistence.pgjooq.Utils;
import de.fraunhofer.iosb.ilt.sta.persistence.pgjooq.relationalpaths.AbstractRecordDatastreams;
import de.fraunhofer.iosb.ilt.sta.persistence.pgjooq.relationalpaths.AbstractTableDatastreams;
import de.fraunhofer.iosb.ilt.sta.persistence.pgjooq.relationalpaths.AbstractTableObservations;
import de.fraunhofer.iosb.ilt.sta.persistence.pgjooq.relationalpaths.QCollection;
import de.fraunhofer.iosb.ilt.sta.query.Query;
import de.fraunhofer.iosb.ilt.sta.util.GeoHelper;
import de.fraunhofer.iosb.ilt.sta.util.IncompleteEntityException;
import de.fraunhofer.iosb.ilt.sta.util.NoSuchEntityException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.geojson.Polygon;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Hylke van der Schaaf
 * @param <J> The type of the ID fields.
 */
public class DatastreamFactory<J> implements EntityFactory<Datastream, J> {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DatastreamFactory.class);

    private final EntityFactories<J> entityFactories;
    private final AbstractTableDatastreams<J> qInstance;
    private final QCollection<J> qCollection;

    public DatastreamFactory(EntityFactories<J> factories, AbstractTableDatastreams<J> qInstance) {
        this.entityFactories = factories;
        this.qInstance = qInstance;
        this.qCollection = factories.qCollection;
    }

    @Override
    public Datastream create(Record tuple, Query query, DataSize dataSize) {
        Set<Property> select = query == null ? Collections.emptySet() : query.getSelect();
        Datastream entity = new Datastream();
        entity.setName(tuple.get(qInstance.name));
        entity.setDescription(tuple.get(qInstance.description));
        J entityId = entityFactories.getIdFromRecord(tuple, qInstance.getId());
        if (entityId != null) {
            entity.setId(entityFactories.idFromObject(entityId));
        }
        entity.setObservationType(tuple.get(qInstance.observationType));
        String observedArea = tuple.get(qInstance.observedAreaText);
        if (observedArea != null) {
            try {
                Polygon polygon = GeoHelper.parsePolygon(observedArea);
                entity.setObservedArea(polygon);
            } catch (IllegalArgumentException e) {
                // It's not a polygon, probably a point or a line.
            }
        }
        ObservedProperty op = entityFactories.observedProperyFromId(tuple, qInstance.getObsPropertyId());
        entity.setObservedProperty(op);
        OffsetDateTime pTimeStart = tuple.get(qInstance.phenomenonTimeStart);
        OffsetDateTime pTimeEnd = tuple.get(qInstance.phenomenonTimeEnd);
        if (pTimeStart != null && pTimeEnd != null) {
            entity.setPhenomenonTime(Utils.intervalFromTimes(pTimeStart, pTimeEnd));
        }
        OffsetDateTime rTimeStart = tuple.get(qInstance.resultTimeStart);
        OffsetDateTime rTimeEnd = tuple.get(qInstance.resultTimeEnd);
        if (rTimeStart != null && rTimeEnd != null) {
            entity.setResultTime(Utils.intervalFromTimes(rTimeStart, rTimeEnd));
        }
        if (select.isEmpty() || select.contains(EntityProperty.PROPERTIES)) {
            String props = tuple.get(qInstance.properties);
            entity.setProperties(Utils.jsonToObject(props, Map.class));
        }
        entity.setSensor(entityFactories.sensorFromId(tuple, qInstance.getSensorId()));
        entity.setThing(entityFactories.thingFromId(tuple, qInstance.getThingId()));
        entity.setUnitOfMeasurement(new UnitOfMeasurement(tuple.get(qInstance.unitName), tuple.get(qInstance.unitSymbol), tuple.get(qInstance.unitDefinition)));
        return entity;
    }

    @Override
    public boolean insert(PostgresPersistenceManager<J> pm, Datastream ds) throws NoSuchEntityException, IncompleteEntityException {
        // First check ObservedPropery, Sensor and Thing
        ObservedProperty op = ds.getObservedProperty();
        entityFactories.entityExistsOrCreate(pm, op);

        Sensor s = ds.getSensor();
        entityFactories.entityExistsOrCreate(pm, s);

        Thing t = ds.getThing();
        entityFactories.entityExistsOrCreate(pm, t);

        DSLContext dslContext = pm.createDdslContext();

        AbstractTableDatastreams<J> qd = qCollection.qDatastreams;
        AbstractRecordDatastreams<J> newDatastream = dslContext.newRecord(qd);

        newDatastream.set(qd.name, ds.getName());
        newDatastream.set(qd.description, ds.getDescription());
        newDatastream.set(qd.observationType, ds.getObservationType());
        newDatastream.set(qd.unitDefinition, ds.getUnitOfMeasurement().getDefinition());
        newDatastream.set(qd.unitName, ds.getUnitOfMeasurement().getName());
        newDatastream.set(qd.unitSymbol, ds.getUnitOfMeasurement().getSymbol());
        newDatastream.set(qd.properties, EntityFactories.objectToJson(ds.getProperties()));

        newDatastream.set(qd.phenomenonTimeStart, PostgresPersistenceManager.DATETIME_MAX);
        newDatastream.set(qd.phenomenonTimeEnd, PostgresPersistenceManager.DATETIME_MIN);
        newDatastream.set(qd.resultTimeStart, PostgresPersistenceManager.DATETIME_MAX);
        newDatastream.set(qd.resultTimeEnd, PostgresPersistenceManager.DATETIME_MIN);

        newDatastream.set(qd.getObsPropertyId(), (J) op.getId().getValue());
        newDatastream.set(qd.getSensorId(), (J) s.getId().getValue());
        newDatastream.set(qd.getThingId(), (J) t.getId().getValue());

        entityFactories.insertUserDefinedId(pm, newDatastream, qd.getId(), ds);

        newDatastream.store();
        J datastreamId = newDatastream.getId();
        LOGGER.debug("Inserted datastream. Created id = {}.", datastreamId);
        ds.setId(entityFactories.idFromObject(datastreamId));

        // Create Observations, if any.
        for (Observation o : ds.getObservations()) {
            o.setDatastream(new Datastream(ds.getId()));
            o.complete();
            pm.insert(o);
        }

        return true;
    }

    @Override
    public EntityChangedMessage update(PostgresPersistenceManager<J> pm, Datastream datastream, J dsId) throws NoSuchEntityException, IncompleteEntityException {

        DSLContext dslContext = pm.createDdslContext();
        AbstractTableDatastreams<J> table = qCollection.qDatastreams;

        AbstractRecordDatastreams<J> update = dslContext.newRecord(table);

        EntityChangedMessage message = new EntityChangedMessage();

        updateName(datastream, update, table, message);
        updateDescription(datastream, update, table, message);
        updateObservationType(datastream, update, table, message);
        updateProperties(datastream, update, table, message);
        updateObservedProperty(datastream, pm, update, table, message);
        updateSensor(datastream, pm, update, table, message);
        updateThing(datastream, pm, update, table, message);
        updateUnitOfMeasurement(datastream, update, table, message);
        update.setId(dsId);

        long count = 0;
        if (update.changed()) {
            count = update.store();
        }
        if (count > 1) {
            LOGGER.error("Updating Datastream {} caused {} rows to change!", dsId, count);
            throw new IllegalStateException(CHANGED_MULTIPLE_ROWS);
        }

        linkExistingObservations(datastream, pm, dslContext, dsId);

        LOGGER.debug("Updated Datastream {}", dsId);
        return message;
    }

    private void updateUnitOfMeasurement(Datastream datastream, AbstractRecordDatastreams update, AbstractTableDatastreams<J> qd, EntityChangedMessage message) throws IncompleteEntityException {
        if (datastream.isSetUnitOfMeasurement()) {
            if (datastream.getUnitOfMeasurement() == null) {
                throw new IncompleteEntityException("unitOfMeasurement" + EntityFactories.CAN_NOT_BE_NULL);
            }
            UnitOfMeasurement uom = datastream.getUnitOfMeasurement();
            update.set(qd.unitDefinition, uom.getDefinition());
            update.set(qd.unitName, uom.getName());
            update.set(qd.unitSymbol, uom.getSymbol());
            message.addField(EntityProperty.UNITOFMEASUREMENT);
        }
    }

    private void updateThing(Datastream datastream, PostgresPersistenceManager<J> pm, AbstractRecordDatastreams update, AbstractTableDatastreams<J> qd, EntityChangedMessage message) throws NoSuchEntityException {
        if (datastream.isSetThing()) {
            if (!entityFactories.entityExists(pm, datastream.getThing())) {
                throw new NoSuchEntityException("Thing with no id or not found.");
            }
            update.set(qd.getThingId(), (J) datastream.getThing().getId().getValue());
            message.addField(NavigationProperty.THING);
        }
    }

    private void updateSensor(Datastream datastream, PostgresPersistenceManager<J> pm, AbstractRecordDatastreams update, AbstractTableDatastreams<J> qd, EntityChangedMessage message) throws NoSuchEntityException {
        if (datastream.isSetSensor()) {
            if (!entityFactories.entityExists(pm, datastream.getSensor())) {
                throw new NoSuchEntityException("Sensor with no id or not found.");
            }
            update.set(qd.getSensorId(), (J) datastream.getSensor().getId().getValue());
            message.addField(NavigationProperty.SENSOR);
        }
    }

    private void updateObservedProperty(Datastream datastream, PostgresPersistenceManager<J> pm, AbstractRecordDatastreams update, AbstractTableDatastreams<J> qd, EntityChangedMessage message) throws NoSuchEntityException {
        if (datastream.isSetObservedProperty()) {
            if (!entityFactories.entityExists(pm, datastream.getObservedProperty())) {
                throw new NoSuchEntityException("ObservedProperty with no id or not found.");
            }
            update.set(qd.getObsPropertyId(), (J) datastream.getObservedProperty().getId().getValue());
            message.addField(NavigationProperty.OBSERVEDPROPERTY);
        }
    }

    private void updateProperties(Datastream datastream, AbstractRecordDatastreams update, AbstractTableDatastreams<J> qd, EntityChangedMessage message) {
        if (datastream.isSetProperties()) {
            update.set(qd.properties, EntityFactories.objectToJson(datastream.getProperties()));
            message.addField(EntityProperty.PROPERTIES);
        }
    }

    private void updateObservationType(Datastream datastream, AbstractRecordDatastreams update, AbstractTableDatastreams<J> qd, EntityChangedMessage message) throws IncompleteEntityException {
        if (datastream.isSetObservationType()) {
            if (datastream.getObservationType() == null) {
                throw new IncompleteEntityException("observationType" + CAN_NOT_BE_NULL);
            }
            update.set(qd.observationType, datastream.getObservationType());
            message.addField(EntityProperty.OBSERVATIONTYPE);
        }
    }

    private void updateDescription(Datastream datastream, AbstractRecordDatastreams update, AbstractTableDatastreams<J> qd, EntityChangedMessage message) throws IncompleteEntityException {
        if (datastream.isSetDescription()) {
            if (datastream.getDescription() == null) {
                throw new IncompleteEntityException(EntityProperty.DESCRIPTION.jsonName + CAN_NOT_BE_NULL);
            }
            update.set(qd.description, datastream.getDescription());
            message.addField(EntityProperty.DESCRIPTION);
        }
    }

    private void updateName(Datastream d, AbstractRecordDatastreams update, AbstractTableDatastreams<J> qd, EntityChangedMessage message) throws IncompleteEntityException {
        if (d.isSetName()) {
            if (d.getName() == null) {
                throw new IncompleteEntityException("name" + CAN_NOT_BE_NULL);
            }
            update.set(qd.name, d.getName());
            message.addField(EntityProperty.NAME);
        }
    }

    private void linkExistingObservations(Datastream d, PostgresPersistenceManager<J> pm, DSLContext qFactory, J dsId) throws NoSuchEntityException {
        for (Observation o : d.getObservations()) {
            if (o.getId() == null || !entityFactories.entityExists(pm, o)) {
                throw new NoSuchEntityException(EntityType.OBSERVATION.entityName + NO_ID_OR_NOT_FOUND);
            }
            J obsId = (J) o.getId().getValue();
            AbstractTableObservations<J> tableObs = qCollection.qObservations;
            long oCount = qFactory.update(tableObs)
                    .set(tableObs.getDatastreamId(), dsId)
                    .where(tableObs.getId().eq(obsId))
                    .execute();
            if (oCount > 0) {
                LOGGER.debug("Assigned datastream {} to Observation {}.", dsId, obsId);
            }
        }
    }

    @Override
    public void delete(PostgresPersistenceManager<J> pm, J entityId) throws NoSuchEntityException {
        long count = pm.createDdslContext()
                .delete(qInstance)
                .where(qInstance.getId().eq(entityId))
                .execute();
        if (count == 0) {
            throw new NoSuchEntityException("Datastream " + entityId + " not found.");
        }
    }

    @Override
    public Field<J> getPrimaryKey() {
        return qInstance.getId();
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DATASTREAM;
    }

}
