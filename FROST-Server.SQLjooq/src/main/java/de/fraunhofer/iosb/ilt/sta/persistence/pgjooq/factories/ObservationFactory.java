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
import de.fraunhofer.iosb.ilt.sta.model.FeatureOfInterest;
import de.fraunhofer.iosb.ilt.sta.model.MultiDatastream;
import de.fraunhofer.iosb.ilt.sta.model.Observation;
import de.fraunhofer.iosb.ilt.sta.model.core.Id;
import de.fraunhofer.iosb.ilt.sta.model.ext.TimeInstant;
import de.fraunhofer.iosb.ilt.sta.model.ext.TimeValue;
import de.fraunhofer.iosb.ilt.sta.path.EntityProperty;
import de.fraunhofer.iosb.ilt.sta.path.EntityType;
import de.fraunhofer.iosb.ilt.sta.path.NavigationProperty;
import de.fraunhofer.iosb.ilt.sta.path.Property;
import de.fraunhofer.iosb.ilt.sta.persistence.pgjooq.DataSize;
import de.fraunhofer.iosb.ilt.sta.persistence.pgjooq.EntityFactories;
import static de.fraunhofer.iosb.ilt.sta.persistence.pgjooq.EntityFactories.CAN_NOT_BE_NULL;
import static de.fraunhofer.iosb.ilt.sta.persistence.pgjooq.EntityFactories.CHANGED_MULTIPLE_ROWS;
import de.fraunhofer.iosb.ilt.sta.persistence.pgjooq.PostgresPersistenceManager;
import de.fraunhofer.iosb.ilt.sta.persistence.pgjooq.ResultType;
import de.fraunhofer.iosb.ilt.sta.persistence.pgjooq.Utils;
import de.fraunhofer.iosb.ilt.sta.persistence.pgjooq.relationalpaths.AbstractRecordObservations;
import de.fraunhofer.iosb.ilt.sta.persistence.pgjooq.relationalpaths.AbstractTableMultiDatastreamsObsProperties;
import de.fraunhofer.iosb.ilt.sta.persistence.pgjooq.relationalpaths.AbstractTableObservations;
import de.fraunhofer.iosb.ilt.sta.persistence.pgjooq.relationalpaths.QCollection;
import de.fraunhofer.iosb.ilt.sta.query.Query;
import de.fraunhofer.iosb.ilt.sta.util.IncompleteEntityException;
import de.fraunhofer.iosb.ilt.sta.util.NoSuchEntityException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Hylke van der Schaaf
 *
 * @param <J> The type of the ID fields.
 */
public class ObservationFactory<J> implements EntityFactory<Observation, J> {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ObservationFactory.class);

    private final EntityFactories<J> entityFactories;
    private final AbstractTableObservations<J> qInstance;
    private final QCollection<J> qCollection;

    public ObservationFactory(EntityFactories<J> factories, AbstractTableObservations<J> qInstance) {
        this.entityFactories = factories;
        this.qInstance = qInstance;
        this.qCollection = factories.qCollection;
    }

    @Override
    public Observation create(Record tuple, Query query, DataSize dataSize) {
        Observation entity = new Observation();
        Set<Property> select = query == null ? Collections.emptySet() : query.getSelect();

        J dsId = entityFactories.getIdFromRecord(tuple, qInstance.getDatastreamId());
        if (dsId != null) {
            entity.setDatastream(entityFactories.datastreamFromId(dsId));
        }

        J mDsId = entityFactories.getIdFromRecord(tuple, qInstance.getMultiDatastreamId());
        if (mDsId != null) {
            entity.setMultiDatastream(entityFactories.multiDatastreamFromId(mDsId));
        }

        entity.setFeatureOfInterest(entityFactories.featureOfInterestFromId(tuple, qInstance.getFeatureId()));

        J id = entityFactories.getIdFromRecord(tuple, qInstance.getId());
        if (id != null) {
            entity.setId(entityFactories.idFromObject(id));
        }

        if (select.isEmpty() || select.contains(EntityProperty.PARAMETERS)) {
            String props = tuple.get(qInstance.parameters);
            dataSize.increase(props == null ? 0 : props.length());
            entity.setParameters(Utils.jsonToObject(props, Map.class));
        }

        OffsetDateTime pTimeStart = tuple.get(qInstance.phenomenonTimeStart);
        OffsetDateTime pTimeEnd = tuple.get(qInstance.phenomenonTimeEnd);
        entity.setPhenomenonTime(Utils.valueFromTimes(pTimeStart, pTimeEnd));

        readResultFromDb(tuple, entity, dataSize, select);
        readResultQuality(select, tuple, dataSize, entity);

        entity.setResultTime(Utils.instantFromTime(tuple.get(qInstance.resultTime)));
        OffsetDateTime vTimeStart = tuple.get(qInstance.validTimeStart);
        OffsetDateTime vTimeEnd = tuple.get(qInstance.validTimeEnd);
        if (vTimeStart != null && vTimeEnd != null) {
            entity.setValidTime(Utils.intervalFromTimes(vTimeStart, vTimeEnd));
        }
        return entity;
    }

    private void readResultQuality(Set<Property> select, Record tuple, DataSize dataSize, Observation entity) {
        if (select.isEmpty() || select.contains(EntityProperty.RESULTQUALITY)) {
            String resultQuality = tuple.get(qInstance.resultQuality);
            dataSize.increase(resultQuality == null ? 0 : resultQuality.length());
            entity.setResultQuality(Utils.jsonToObject(resultQuality, Object.class));
        }
    }

    private void readResultFromDb(Record tuple, Observation entity, DataSize dataSize, Set<Property> select) {
        if (!select.isEmpty() && !select.contains(EntityProperty.RESULT)) {
            return;
        }
        Short resultTypeOrd = tuple.get(qInstance.resultType);
        if (resultTypeOrd != null) {
            ResultType resultType = ResultType.fromSqlValue(resultTypeOrd);
            switch (resultType) {
                case BOOLEAN:
                    entity.setResult(tuple.get(qInstance.resultBoolean));
                    break;
                case NUMBER:
                    try {
                        entity.setResult(new BigDecimal(tuple.get(qInstance.resultString)));
                    } catch (NumberFormatException e) {
                        // It was not a Number? Use the double value.
                        entity.setResult(tuple.get(qInstance.resultNumber));
                    }
                    break;
                case OBJECT_ARRAY:
                    String jsonData = tuple.get(qInstance.resultJson);
                    dataSize.increase(jsonData == null ? 0 : jsonData.length());
                    entity.setResult(Utils.jsonToTree(jsonData));
                    break;
                case STRING:
                    String stringData = tuple.get(qInstance.resultString);
                    dataSize.increase(stringData == null ? 0 : stringData.length());
                    entity.setResult(stringData);
                    break;
            }
        }
    }

    @Override
    public boolean insert(PostgresPersistenceManager<J> pm, Observation newObservation) throws NoSuchEntityException, IncompleteEntityException {
        Datastream ds = newObservation.getDatastream();
        MultiDatastream mds = newObservation.getMultiDatastream();
        Id streamId;
        boolean newIsMultiDatastream = false;
        if (ds != null) {
            entityFactories.entityExistsOrCreate(pm, ds);
            streamId = ds.getId();
        } else if (mds != null) {
            entityFactories.entityExistsOrCreate(pm, mds);
            streamId = mds.getId();
            newIsMultiDatastream = true;
        } else {
            throw new IncompleteEntityException("Missing Datastream or MultiDatastream.");
        }

        FeatureOfInterest f = newObservation.getFeatureOfInterest();
        if (f == null) {
            f = entityFactories.generateFeatureOfInterest(pm, streamId, newIsMultiDatastream);
        } else {
            entityFactories.entityExistsOrCreate(pm, f);
        }

        DSLContext dslContext = pm.createDdslContext();
        AbstractTableObservations<J> qo = qCollection.qObservations;
        AbstractRecordObservations<J> insert = dslContext.newRecord(qo);

        if (ds != null) {
            insert.set(qo.getDatastreamId(), (J) ds.getId().getValue());
        }
        if (mds != null) {
            insert.set(qo.getMultiDatastreamId(), (J) mds.getId().getValue());
        }

        TimeValue phenomenonTime = newObservation.getPhenomenonTime();
        if (phenomenonTime == null) {
            phenomenonTime = TimeInstant.now();
        }
        EntityFactories.insertTimeValue(insert, qo.phenomenonTimeStart, qo.phenomenonTimeEnd, phenomenonTime);
        EntityFactories.insertTimeInstant(insert, qo.resultTime, newObservation.getResultTime());
        EntityFactories.insertTimeInterval(insert, qo.validTimeStart, qo.validTimeEnd, newObservation.getValidTime());

        handleResult(newObservation, newIsMultiDatastream, pm, insert, qo);

        if (newObservation.getResultQuality() != null) {
            insert.set(qo.resultQuality, newObservation.getResultQuality().toString());
        }
        insert.set(qo.parameters, EntityFactories.objectToJson(newObservation.getParameters()));
        insert.set(qo.getFeatureId(), (J) f.getId().getValue());

        entityFactories.insertUserDefinedId(pm, insert, qo.getId(), newObservation);

        insert.store();
        J generatedId = insert.getId();
        LOGGER.debug("Inserted Observation. Created id = {}.", generatedId);
        newObservation.setId(entityFactories.idFromObject(generatedId));
        return true;
    }

    @Override
    public EntityChangedMessage update(PostgresPersistenceManager<J> pm, Observation newObservation, J id) throws IncompleteEntityException {
        Observation oldObservation = (Observation) pm.get(EntityType.OBSERVATION, entityFactories.idFromObject(id));

        DSLContext dslContext = pm.createDdslContext();
        AbstractTableObservations<J> qo = qCollection.qObservations;
        AbstractRecordObservations<J> update = dslContext.newRecord(qo);
        EntityChangedMessage message = new EntityChangedMessage();

        boolean newHasDatastream = checkDatastreamSet(newObservation, oldObservation, message, update, qo, pm);
        boolean newIsMultiDatastream = checkMultiDatastreamSet(oldObservation, newObservation, message, update, qo, pm);

        if (newHasDatastream == newIsMultiDatastream) {
            throw new IllegalArgumentException("Observation must have either a Datastream or a MultiDatastream.");
        }
        if (newObservation.isSetFeatureOfInterest()) {
            if (!entityFactories.entityExists(pm, newObservation.getFeatureOfInterest())) {
                throw new IncompleteEntityException("FeatureOfInterest not found.");
            }
            update.set(qo.getFeatureId(), (J) newObservation.getFeatureOfInterest().getId().getValue());
            message.addField(NavigationProperty.FEATUREOFINTEREST);
        }
        if (newObservation.isSetParameters()) {
            update.set(qo.parameters, EntityFactories.objectToJson(newObservation.getParameters()));
            message.addField(EntityProperty.PARAMETERS);
        }
        if (newObservation.isSetPhenomenonTime()) {
            if (newObservation.getPhenomenonTime() == null) {
                throw new IncompleteEntityException("phenomenonTime" + CAN_NOT_BE_NULL);
            }
            EntityFactories.insertTimeValue(update, qo.phenomenonTimeStart, qo.phenomenonTimeEnd, newObservation.getPhenomenonTime());
            message.addField(EntityProperty.PHENOMENONTIME);
        }

        if (newObservation.isSetResult()) {
            handleResult(newObservation, newIsMultiDatastream, pm, update, qo);
            message.addField(EntityProperty.RESULT);
        }

        if (newObservation.isSetResultQuality()) {
            update.set(qo.resultQuality, EntityFactories.objectToJson(newObservation.getResultQuality()));
            message.addField(EntityProperty.RESULTQUALITY);
        }
        if (newObservation.isSetResultTime()) {
            EntityFactories.insertTimeInstant(update, qo.resultTime, newObservation.getResultTime());
            message.addField(EntityProperty.RESULTTIME);
        }
        if (newObservation.isSetValidTime()) {
            EntityFactories.insertTimeInterval(update, qo.validTimeStart, qo.validTimeEnd, newObservation.getValidTime());
            message.addField(EntityProperty.VALIDTIME);
        }
        update.setId(id);
        long count = 0;
        if (update.changed()) {
            count = update.store();
        }
        if (count > 1) {
            LOGGER.error("Updating Observation {} caused {} rows to change!", id, count);
            throw new IllegalStateException(CHANGED_MULTIPLE_ROWS);
        }
        LOGGER.debug("Updated Observation {}", id);
        return message;
    }

    private void handleResult(Observation newObservation, boolean newIsMultiDatastream, PostgresPersistenceManager<J> pm, AbstractRecordObservations<J> record, AbstractTableObservations<J> qo) {
        Object result = newObservation.getResult();
        if (newIsMultiDatastream) {
            if (!(result instanceof List)) {
                throw new IllegalArgumentException("Multidatastream only accepts array results.");
            }
            List list = (List) result;
            MultiDatastream mds = newObservation.getMultiDatastream();
            J mdsId = (J) mds.getId().getValue();
            AbstractTableMultiDatastreamsObsProperties<J> tableMdsOps = qCollection.qMultiDatastreamsObsProperties;
            Integer count = pm.createDdslContext()
                    .selectCount()
                    .from(tableMdsOps)
                    .where(tableMdsOps.getMultiDatastreamId().eq(mdsId))
                    .fetchOne().component1();
            if (count != list.size()) {
                throw new IllegalArgumentException("Size of result array (" + list.size() + ") must match number of observed properties (" + count + ") in the MultiDatastream.");
            }
        }

        if (result instanceof Number) {
            record.set(qo.resultType, ResultType.NUMBER.sqlValue());
            record.set(qo.resultString, result.toString());
            record.set(qo.resultNumber, ((Number) result).doubleValue());
            record.set(qo.resultBoolean, null);
            record.set(qo.resultJson, null);
        } else if (result instanceof Boolean) {
            record.set(qo.resultType, ResultType.BOOLEAN.sqlValue());
            record.set(qo.resultString, result.toString());
            record.set(qo.resultBoolean, (Boolean) result);
            record.set(qo.resultNumber, null);
            record.set(qo.resultJson, null);
        } else if (result instanceof String) {
            record.set(qo.resultType, ResultType.STRING.sqlValue());
            record.set(qo.resultString, result.toString());
            record.set(qo.resultNumber, null);
            record.set(qo.resultBoolean, null);
            record.set(qo.resultJson, null);
        } else {
            record.set(qo.resultType, ResultType.OBJECT_ARRAY.sqlValue());
            record.set(qo.resultJson, EntityFactories.objectToJson(result));
            record.set(qo.resultString, null);
            record.set(qo.resultNumber, null);
            record.set(qo.resultBoolean, null);
        }
    }

    private boolean checkMultiDatastreamSet(Observation oldObservation, Observation newObservation, EntityChangedMessage message, AbstractRecordObservations<J> update, AbstractTableObservations<J> qo, PostgresPersistenceManager<J> pm) throws IncompleteEntityException {
        MultiDatastream mds = oldObservation.getMultiDatastream();
        boolean newHasMultiDatastream = mds != null;
        if (newObservation.isSetMultiDatastream()) {
            mds = newObservation.getMultiDatastream();
            if (mds == null) {
                newHasMultiDatastream = false;
                update.set(qo.getMultiDatastreamId(), null);
                message.addField(NavigationProperty.MULTIDATASTREAM);
            } else {
                if (!entityFactories.entityExists(pm, mds)) {
                    throw new IncompleteEntityException("MultiDatastream not found.");
                }
                newHasMultiDatastream = true;
                update.set(qo.getMultiDatastreamId(), (J) mds.getId().getValue());
                message.addField(NavigationProperty.MULTIDATASTREAM);
            }
        }
        return newHasMultiDatastream;
    }

    private boolean checkDatastreamSet(Observation newObservation, Observation oldObservation, EntityChangedMessage message, AbstractRecordObservations<J> update, AbstractTableObservations<J> qo, PostgresPersistenceManager<J> pm) throws IncompleteEntityException {
        Datastream ds = oldObservation.getDatastream();
        boolean newHasDatastream = ds != null;
        if (newObservation.isSetDatastream()) {
            if (newObservation.getDatastream() == null) {
                newHasDatastream = false;
                update.set(qo.getDatastreamId(), null);
                message.addField(NavigationProperty.DATASTREAM);
            } else {
                if (!entityFactories.entityExists(pm, newObservation.getDatastream())) {
                    throw new IncompleteEntityException("Datastream not found.");
                }
                newHasDatastream = true;
                ds = newObservation.getDatastream();
                update.set(qo.getDatastreamId(), (J) ds.getId().getValue());
                message.addField(NavigationProperty.DATASTREAM);
            }
        }
        return newHasDatastream;
    }

    @Override
    public void delete(PostgresPersistenceManager<J> pm, J entityId) throws NoSuchEntityException {
        long count = pm.createDdslContext()
                .delete(qInstance)
                .where(qInstance.getId().eq(entityId))
                .execute();
        if (count == 0) {
            throw new NoSuchEntityException("Observation " + entityId + " not found.");
        }
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.OBSERVATION;
    }

    @Override
    public Field<J> getPrimaryKey() {
        return qInstance.getId();
    }

}
