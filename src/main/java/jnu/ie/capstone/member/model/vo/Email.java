package jnu.ie.capstone.member.model.vo;

import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import java.util.regex.Pattern;
import jnu.ie.capstone.common.converter.StringEncryptConverter;
import jnu.ie.capstone.common.exception.client.BadDataSyntaxException;

@Embeddable
public record Email(
        @Convert(converter = StringEncryptConverter.class)
        String value
) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    public Email {
        validate(value);
    }

    private static void validate(String value) {
        if (value == null || !EMAIL_PATTERN.matcher(value).matches()) {
            throw new BadDataSyntaxException("올바른 이메일 형식이 아닙니다.");
        }
    }
}