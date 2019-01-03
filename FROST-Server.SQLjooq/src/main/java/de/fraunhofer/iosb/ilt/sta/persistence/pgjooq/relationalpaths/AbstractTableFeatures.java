package de.fraunhofer.iosb.ilt.sta.persistence.pgjooq.relationalpaths;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Schema;
import org.jooq.TableField;
import org.jooq.codegen.maven.example.Public;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;

public abstract class AbstractTableFeatures<J> extends TableImpl<AbstractRecordFeatures<J>> implements StaTable<J, AbstractRecordFeatures<J>> {

    private static final long serialVersionUID = 750481677;

    public abstract TableField<AbstractRecordFeatures<J>, J> getId();

    /**
     * The column <code>public.FEATURES.DESCRIPTION</code>.
     */
    public final TableField<AbstractRecordFeatures<J>, String> description = createField("DESCRIPTION", org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * The column <code>public.FEATURES.ENCODING_TYPE</code>.
     */
    public final TableField<AbstractRecordFeatures<J>, String> encodingType = createField("ENCODING_TYPE", org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * The column <code>public.FEATURES.FEATURE</code>.
     */
    public final TableField<AbstractRecordFeatures<J>, String> feature = createField("FEATURE", org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * @deprecated Unknown data type. Please define an explicit
     * {@link org.jooq.Binding} to specify how this type should be handled.
     * Deprecation can be turned off using
     * {@literal <deprecationOnUnknownTypes/>} in your code generator
     * configuration.
     */
    @java.lang.Deprecated
    public final TableField<AbstractRecordFeatures<J>, Object> geom = createField("GEOM", org.jooq.impl.DefaultDataType.getDefaultDataType("\"public\".\"geometry\""), this, "");

    /**
     * The column <code>public.FEATURES.NAME</code>.
     */
    public final TableField<AbstractRecordFeatures<J>, String> name = createField("NAME", org.jooq.impl.SQLDataType.CLOB.defaultValue(org.jooq.impl.DSL.field("'no name'::text", org.jooq.impl.SQLDataType.CLOB)), this, "");

    /**
     * The column <code>public.FEATURES.PROPERTIES</code>.
     */
    public final TableField<AbstractRecordFeatures<J>, String> properties = createField("PROPERTIES", org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * Create a <code>public.FEATURES</code> table reference
     */
    protected AbstractTableFeatures() {
        this(DSL.name("FEATURES"), null);
    }

    protected AbstractTableFeatures(Name alias, AbstractTableFeatures<J> aliased) {
        this(alias, aliased, null);
    }

    protected AbstractTableFeatures(Name alias, AbstractTableFeatures<J> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema getSchema() {
        return Public.PUBLIC;
    }

    @Override
    public abstract AbstractTableFeatures<J> as(Name as);

    @Override
    public abstract AbstractTableFeatures<J> as(String alias);

}
