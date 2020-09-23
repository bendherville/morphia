package dev.morphia.mapping.codec.pojo;

import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PrePersist;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.codec.DocumentWriter;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.IdGenerator;
import org.bson.codecs.ObjectIdGenerator;
import org.bson.types.ObjectId;

import java.util.Collection;
import java.util.Map;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;

/**
 * @since 2.0
 */
class EntityEncoder<T> implements org.bson.codecs.Encoder<T> {
    public static final ObjectIdGenerator OBJECT_ID_GENERATOR = new ObjectIdGenerator();
    private final MorphiaCodec<T> morphiaCodec;
    private IdGenerator idGenerator;

    EntityEncoder(MorphiaCodec<T> morphiaCodec) {
        this.morphiaCodec = morphiaCodec;
    }

    @Override
    public void encode(BsonWriter writer, T value, EncoderContext encoderContext) {
        MappedClass mappedClass = morphiaCodec.getMappedClass();
        if (mappedClass.hasLifecycle(PostPersist.class)
            || mappedClass.hasLifecycle(PrePersist.class)
            || morphiaCodec.getMapper().hasInterceptors()) {

            encodeWithLifecycle(writer, value, encoderContext);
        } else {
            encodeEntity(writer, value, encoderContext);
        }
    }

    @Override
    public Class<T> getEncoderClass() {
        return morphiaCodec.getEncoderClass();
    }

    private <S, V> boolean areEquivalentTypes(Class<S> t1, Class<V> t2) {
        return t1.equals(t2)
               || Collection.class.isAssignableFrom(t1) && Collection.class.isAssignableFrom(t2)
               || Map.class.isAssignableFrom(t1) && Map.class.isAssignableFrom(t2);
    }

    @SuppressWarnings("unchecked")
    private void encodeEntity(BsonWriter writer, T value, EncoderContext encoderContext) {
        if (areEquivalentTypes(value.getClass(), morphiaCodec.getEntityModel().getType())) {
            document(writer, () -> {

                FieldModel<?> idModel = morphiaCodec.getEntityModel().getIdModel();
                encodeIdProperty(writer, value, encoderContext, idModel);

                if (morphiaCodec.getEntityModel().useDiscriminator()) {
                    writer.writeString(morphiaCodec.getEntityModel().getDiscriminatorKey(),
                        morphiaCodec.getEntityModel().getDiscriminator());
                }

                for (FieldModel<?> fieldModel : morphiaCodec.getEntityModel().getFieldModels()) {
                    if (fieldModel.equals(idModel)) {
                        continue;
                    }
                    encodeProperty(writer, value, encoderContext, fieldModel);
                }
            });
        } else {
            morphiaCodec.getRegistry().get((Class<T>) value.getClass())
                        .encode(writer, value, encoderContext);
        }
    }

    private <S> void encodeIdProperty(BsonWriter writer, T instance, EncoderContext encoderContext,
                                      FieldModel<S> idModel) {
        if (idModel != null) {
            IdGenerator generator = getIdGenerator();
            if (generator == null) {
                encodeProperty(writer, instance, encoderContext, idModel);
            } else {
                S id = idModel.getAccessor().get(instance);
                if (id == null && encoderContext.isEncodingCollectibleDocument()) {
                    id = (S) generator.generate();
                    idModel.getAccessor().set(instance, id);
                }
                encodeValue(writer, encoderContext, idModel, id);
            }
        }
    }

    private <S> void encodeProperty(BsonWriter writer, T instance, EncoderContext encoderContext,
                                    FieldModel<S> model) {
        if (model != null) {
            S value = model.getAccessor().get(instance);
            encodeValue(writer, encoderContext, model, value);
        }
    }

    private <S> void encodeValue(BsonWriter writer, EncoderContext encoderContext, FieldModel<S> model,
                                 S propertyValue) {
        if (model.shouldSerialize(propertyValue)) {
            writer.writeName(model.getMappedName());
            if (propertyValue == null) {
                writer.writeNull();
            } else {
                encoderContext.encodeWithChildContext(model.getCachedCodec(), writer, propertyValue);
            }
        }
    }

    private void encodeWithLifecycle(BsonWriter writer, T value, EncoderContext encoderContext) {
        Document document = new Document();
        morphiaCodec.getMappedClass().callLifecycleMethods(PrePersist.class, value, document, morphiaCodec.getMapper());

        final DocumentWriter documentWriter = new DocumentWriter(document);
        encodeEntity(documentWriter, value, encoderContext);
        document = documentWriter.getDocument();
        morphiaCodec.getMappedClass().callLifecycleMethods(PostPersist.class, value, document, morphiaCodec.getMapper());

        morphiaCodec.getRegistry().get(Document.class).encode(writer, document, encoderContext);
    }

    private IdGenerator getIdGenerator() {
        if (idGenerator == null) {
            FieldModel<?> idModel = morphiaCodec.getEntityModel().getIdModel();
            if (idModel.getNormalizedType().isAssignableFrom(ObjectId.class)) {
                idGenerator = OBJECT_ID_GENERATOR;
            }
        }

        return idGenerator;
    }

}
