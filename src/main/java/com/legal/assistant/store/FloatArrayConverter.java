package com.legal.assistant.store;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

@Converter
public class FloatArrayConverter implements AttributeConverter<float[], byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) return null;
        ByteBuffer buf = ByteBuffer.allocate(Float.BYTES * attribute.length);
        for (float f : attribute) buf.putFloat(f);
        return buf.array();
    }

    @Override
    public float[] convertToEntityAttribute(byte[] dbData) {
        if (dbData == null) return null;
        FloatBuffer buf = ByteBuffer.wrap(dbData).asFloatBuffer();
        float[] result = new float[buf.capacity()];
        buf.get(result);
        return result;
    }
}
