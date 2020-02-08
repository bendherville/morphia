package dev.morphia.query;


import com.mongodb.client.model.geojson.CoordinateReferenceSystem;
import com.mongodb.client.model.geojson.MultiPolygon;
import com.mongodb.client.model.geojson.Polygon;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
import dev.morphia.utils.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static dev.morphia.query.FilterOperator.ALL;
import static dev.morphia.query.FilterOperator.ELEMENT_MATCH;
import static dev.morphia.query.FilterOperator.EQUAL;
import static dev.morphia.query.FilterOperator.EXISTS;
import static dev.morphia.query.FilterOperator.GEO_WITHIN;
import static dev.morphia.query.FilterOperator.GREATER_THAN;
import static dev.morphia.query.FilterOperator.GREATER_THAN_OR_EQUAL;
import static dev.morphia.query.FilterOperator.IN;
import static dev.morphia.query.FilterOperator.INTERSECTS;
import static dev.morphia.query.FilterOperator.LESS_THAN;
import static dev.morphia.query.FilterOperator.LESS_THAN_OR_EQUAL;
import static dev.morphia.query.FilterOperator.MOD;
import static dev.morphia.query.FilterOperator.NEAR;
import static dev.morphia.query.FilterOperator.NEAR_SPHERE;
import static dev.morphia.query.FilterOperator.NOT_EQUAL;
import static dev.morphia.query.FilterOperator.NOT_IN;
import static dev.morphia.query.FilterOperator.SIZE;
import static dev.morphia.query.FilterOperator.TYPE;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
import static java.util.regex.Pattern.quote;

/**
 * Represents a document field in a query and presents the operations available to querying against that field.  This is an internal
 * class and subject to change without notice.
 *
 * @param <T> the type of the CriteriaContainer
 * @morphia.internal
 */
public class FieldEndImpl<T extends CriteriaContainer> implements FieldEnd<T> {
    private static final Logger LOG = LoggerFactory.getLogger(FieldEndImpl.class);
    private final String field;
    private final T target;
    private Mapper mapper;
    private boolean not;
    private final MappedClass mappedClass;
    private final boolean validating;

    /**
     * Creates a FieldEnd for a particular field.
     *
     * @param field       the field to consider
     * @param target      the CriteriaContainer
     * @param mappedClass the mapped class
     * @param validating  validate names or not
     */
    protected FieldEndImpl(final Mapper mapper, final String field, final T target, MappedClass mappedClass, final boolean validating) {
        this.mapper = mapper;
        this.field = field;
        this.target = target;
        this.mappedClass = mappedClass;
        this.validating = validating;
    }

    @Override
    public T contains(final String string) {
        Assert.parametersNotNull("val", string);
        return addCriteria(EQUAL, compile(quote(string)));
    }

    @Override
    public T containsIgnoreCase(final String string) {
        Assert.parametersNotNull("val", string);
        return addCriteria(EQUAL, compile(quote(string), CASE_INSENSITIVE));
    }

    @Override
    public T doesNotExist() {
        return addCriteria(EXISTS, false);
    }

    @Override
    public T endsWith(final String suffix) {
        Assert.parametersNotNull("val", suffix);
        return addCriteria(EQUAL, compile(quote(suffix) + "$"));
    }

    @Override
    public T endsWithIgnoreCase(final String suffix) {
        Assert.parametersNotNull("val", suffix);
        return addCriteria(EQUAL, compile(quote(suffix) + "$", CASE_INSENSITIVE));
    }

    @Override
    public T equal(final Object val) {
        return addCriteria(EQUAL, val);
    }

    @Override
    public T equalIgnoreCase(final Object val) {
        Assert.parametersNotNull("val", val);
        return addCriteria(EQUAL, compile("^" + quote(val.toString()) + "$", CASE_INSENSITIVE));
    }

    @Override
    public T exists() {
        return addCriteria(EXISTS, true);
    }

    @Override
    public T greaterThan(final Object val) {
        Assert.parametersNotNull("val", val);
        return addCriteria(GREATER_THAN, val);
    }

    @Override
    public T greaterThanOrEq(final Object val) {
        Assert.parametersNotNull("val", val);
        return addCriteria(GREATER_THAN_OR_EQUAL, val);
    }

    @Override
    public T hasAllOf(final Iterable<?> values) {
        Assert.parametersNotNull("values", values);
        Assert.parameterNotEmpty("values", values);
        return addCriteria(ALL, values);
    }

    @Override
    public T hasAnyOf(final Iterable<?> values) {
        Assert.parametersNotNull("values", values);
        Assert.parameterNotEmpty("values", values);

        return addCriteria(IN, values);
    }

    @Override
    public T hasNoneOf(final Iterable<?> values) {
        Assert.parametersNotNull("values", values);
        Assert.parameterNotEmpty("values", values);
        return addCriteria(NOT_IN, values);
    }

    @Override
    public T elemMatch(final Query query) {
        Assert.parametersNotNull("query", query);
        return addCriteria(ELEMENT_MATCH, query, not);
    }

    @Override
    public T hasThisOne(final Object val) {
        return addCriteria(EQUAL, val);
    }

    @Override
    public T in(final Iterable<?> values) {
        return hasAnyOf(values);
    }

    @Override
    public T intersects(final com.mongodb.client.model.geojson.Geometry geometry) {
        target.add(Geo2dSphereCriteria.geo(mapper, field, INTERSECTS, geometry, mappedClass, validating));
        return target;
    }

    @Override
    public T intersects(final com.mongodb.client.model.geojson.Geometry geometry, final CoordinateReferenceSystem crs) {
        target.add(Geo2dSphereCriteria.geo(mapper, field, INTERSECTS, geometry, mappedClass, validating)
                                      .addCoordinateReferenceSystem(crs));
        return target;
    }

    @Override
    public T lessThan(final Object val) {
        Assert.parametersNotNull("val", val);
        return addCriteria(LESS_THAN, val);
    }

    @Override
    public T lessThanOrEq(final Object val) {
        Assert.parametersNotNull("val", val);
        return addCriteria(LESS_THAN_OR_EQUAL, val);
    }

    @Override
    public T mod(final long divisor, final long remainder) {
        return addCriteria(MOD, new long[]{divisor, remainder});
    }

    @Override
    public T near(final double longitude, final double latitude) {
        return near(longitude, latitude, false);
    }

    @Override
    public T near(final double longitude, final double latitude, final boolean spherical) {
        return addGeoCriteria(spherical ? NEAR_SPHERE : NEAR, new double[]{longitude, latitude}, null);
    }

    @Override
    public T near(final double longitude, final double latitude, final double radius) {
        return near(longitude, latitude, radius, false);
    }

    @Override
    public T near(final double longitude, final double latitude, final double radius, final boolean spherical) {
        return addGeoCriteria(spherical ? NEAR_SPHERE : NEAR,
            new double[]{longitude, latitude},
            Map.of("$maxDistance", radius));
    }

    @Override
    public T near(final com.mongodb.client.model.geojson.Point point) {
        target.add(Geo2dSphereCriteria.geo(mapper, field, NEAR, point, mappedClass,
            validating));
        return target;
    }

    @Override
    public T near(final com.mongodb.client.model.geojson.Point point, final Double maxDistance, final Double minDistance) {
        target.add(Geo2dSphereCriteria.geo(mapper, field, NEAR, point, mappedClass,
            validating)
                                      .maxDistance(maxDistance)
                                      .minDistance(minDistance));
        return target;
    }

    @Override
    public T nearSphere(final com.mongodb.client.model.geojson.Point point) {
        return nearSphere(point, null, null);
    }

    @Override
    public T nearSphere(final com.mongodb.client.model.geojson.Point point, final Double maxDistance, final Double minDistance) {
        target.add(Geo2dSphereCriteria.geo(mapper, field, NEAR_SPHERE, point, mappedClass,
            validating)
                                      .maxDistance(maxDistance)
                                      .minDistance(minDistance));
        return target;
    }

    @Override
    public FieldEnd<T> not() {
        not = !not;
        return this;
    }

    @Override
    public T notEqual(final Object val) {
        return addCriteria(NOT_EQUAL, val);
    }

    @Override
    public T notIn(final Iterable<?> values) {
        return hasNoneOf(values);
    }

    @Override
    public T sizeEq(final int val) {
        return addCriteria(SIZE, val);
    }

    @Override
    public T startsWith(final String prefix) {
        Assert.parametersNotNull("val", prefix);
        /*LITERAL*/
        return addCriteria(EQUAL, compile("^" + quote(prefix)));
    }

    @Override
    public T startsWithIgnoreCase(final String prefix) {
        Assert.parametersNotNull("val", prefix);
        /*  | LITERAL */
        return addCriteria(EQUAL, compile("^" + quote(prefix), CASE_INSENSITIVE));
    }

    @Override
    public T type(final Type type) {
        return addCriteria(TYPE, type.val());
    }

    @Override
    public T within(final Shape shape) {
        Assert.parametersNotNull("shape", shape);
        return addCriteria(GEO_WITHIN, shape);
    }

    @Override
    public T within(final Polygon boundary) {
        target.add(Geo2dSphereCriteria.geo(mapper, field, GEO_WITHIN, boundary, mappedClass,
            validating));
        return target;
    }

    @Override
    public T within(final MultiPolygon boundaries) {
        target.add(Geo2dSphereCriteria.geo(mapper, field, GEO_WITHIN, boundaries, mappedClass,
            validating));
        return target;
    }

    @Override
    public T within(final Polygon boundary, final CoordinateReferenceSystem crs) {
        target.add(Geo2dSphereCriteria.geo(mapper, field, GEO_WITHIN, boundary, mappedClass,
            validating)
                                      .addCoordinateReferenceSystem(crs));
        return target;
    }

    @Override
    public T within(final MultiPolygon boundaries, final CoordinateReferenceSystem crs) {
        target.add(Geo2dSphereCriteria.geo(mapper, field, GEO_WITHIN, boundaries, mappedClass,
            validating)
                                      .addCoordinateReferenceSystem(crs));
        return target;
    }

    protected T addCriteria(final FilterOperator op, final Object val) {
        return addCriteria(op, val, not);
    }

    protected T addCriteria(final FilterOperator op, final Object val, final boolean not) {
        target.add(new FieldCriteria(mapper, field, op, val, not, mappedClass, validating));
        return target;
    }

    private T addGeoCriteria(final FilterOperator op, final Object val, final Map<String, Object> opts) {
        if (not) {
            throw new QueryException("Geospatial queries cannot be negated with 'not'.");
        }

        target.add(new Geo2dCriteria(mapper, field, op, val, opts, mappedClass, validating));
        return target;
    }
}
