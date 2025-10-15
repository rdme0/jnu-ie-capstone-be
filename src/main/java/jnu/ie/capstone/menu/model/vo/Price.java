package jnu.ie.capstone.menu.model.vo;

import static jnu.ie.capstone.menu.constant.MenuConstant.MIN_PRICE;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Embeddable;
import jnu.ie.capstone.common.exception.client.BadDataSyntaxException;

@Embeddable
public record Price(
        @JsonValue Long value
) {

    @JsonCreator
    public Price {
        validate(value);
    }

    private void validate(Long value) {
        if (value == null || value < MIN_PRICE) {
            throw new BadDataSyntaxException("가격은 %d보다 작을 수 없습니다.".formatted(MIN_PRICE));
        }
    }

}
