package dev.morphia.aggregation.codecs.stages;

import dev.morphia.MorphiaDatastore;
import dev.morphia.aggregation.stages.Match;
import dev.morphia.query.filters.Filter;

import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;

import static dev.morphia.mapping.codec.expressions.ExpressionCodecHelper.document;

public class MatchCodec extends StageCodec<Match> {

    public MatchCodec(MorphiaDatastore datastore) {
        super(datastore);
    }

    @Override
    public Class<Match> getEncoderClass() {
        return Match.class;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void encodeStage(BsonWriter writer, Match value, EncoderContext encoderContext) {
        document(writer, () -> {
            for (Filter filter : value.getFilters()) {
                filter.encode(getDatastore(), writer, encoderContext);
            }
        });
    }
}
