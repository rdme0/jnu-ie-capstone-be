package jnu.ie.capstone.menu.model.vo;

import static jnu.ie.capstone.menu.constant.MenuConstant.OPTION_NAME_MAX_LENGTH;
import static jnu.ie.capstone.menu.constant.MenuConstant.OPTION_NAME_MIN_LENGTH;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.persistence.Embeddable;
import jnu.ie.capstone.common.exception.client.BadDataLengthException;

@Embeddable
public record OptionName(
        String value
) {

    @JsonCreator
    public OptionName {
        validate(value);
    }

    private void validate(String value) {
        if (value == null || value.length() < OPTION_NAME_MIN_LENGTH
                || value.length() > OPTION_NAME_MAX_LENGTH) {
            throw new BadDataLengthException("옵션 이름", OPTION_NAME_MIN_LENGTH, OPTION_NAME_MAX_LENGTH);
        }
    }
}
