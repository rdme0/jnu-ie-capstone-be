package jnu.ie.capstone.member.model.vo;

import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.regex.Pattern;
import jnu.ie.capstone.common.converter.StringEncryptConverter;
import jnu.ie.capstone.common.exception.client.BadDataSyntaxException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Email {
    @Convert(converter = StringEncryptConverter.class)
    private String value;

    public Email(String value) {
        validateEmail(value);
        this.value = value;
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );



    private void validateEmail(String value) {
        if (value == null || !EMAIL_PATTERN.matcher(value).matches()) {
            throw new BadDataSyntaxException("올바른 이메일 형식이 아닙니다.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Email email = (Email) o;
        return Objects.equals(getValue(), email.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getValue());
    }
}