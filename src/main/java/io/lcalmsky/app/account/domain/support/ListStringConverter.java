package io.lcalmsky.app.account.domain.support;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Converter
public class ListStringConverter implements AttributeConverter<List<String>, String> {
    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        return Optional.ofNullable(attribute)
                .map(a -> String.join(",", a))
                .orElse("");
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        return Stream.of(dbData.split(","))
                .collect(Collectors.toList());
    }
}
