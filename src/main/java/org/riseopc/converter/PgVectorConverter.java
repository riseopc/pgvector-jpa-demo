package org.riseopc.converter;

import org.riseopc.util.VectorUtils;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA 属性转换器：将实体类中的 float[] 转换为 PostgreSQL 的 vector 字符串表示形式 "[0.1, 0.2, ...]"
 * 配合 JDBC URL 中的 stringtype=unspecified，PostgreSQL 驱动会自动将其适配为 vector(dim) 字段。
 */
@Converter(autoApply = false)
public class PgVectorConverter implements AttributeConverter<float[], String> {

    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) {
            return null;
        }
        return VectorUtils.toVectorString(attribute);
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return VectorUtils.parseVectorString(dbData);
    }
}

