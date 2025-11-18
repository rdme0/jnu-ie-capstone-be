package jnu.ie.capstone.store.model.vo;

import static jnu.ie.capstone.store.constant.StoreConstant.STORE_NAME_MAX_LENGTH;
import static jnu.ie.capstone.store.constant.StoreConstant.STORE_NAME_MIN_LENGTH;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Embeddable;
import jnu.ie.capstone.common.exception.client.BadDataLengthException;

@Embeddable
public record StoreName(
        @JsonValue String value
) {

    @JsonCreator
    public StoreName {
        validate(value);
    }

    private void validate(String value) {
        if (value == null || value.length() < STORE_NAME_MIN_LENGTH
                || value.length() > STORE_NAME_MAX_LENGTH) {
            throw new BadDataLengthException("스토어 이름", STORE_NAME_MIN_LENGTH,
                    STORE_NAME_MAX_LENGTH);
        }
    }
}
