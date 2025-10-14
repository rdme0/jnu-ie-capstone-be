package jnu.ie.capstone.menu.model.vo;

import static jnu.ie.capstone.menu.constant.MenuConstant.MENU_NAME_MAX_LENGTH;
import static jnu.ie.capstone.menu.constant.MenuConstant.MENU_NAME_MIN_LENGTH;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.persistence.Embeddable;
import jnu.ie.capstone.common.exception.client.BadDataLengthException;

@Embeddable
public record MenuName(
        String value
) {

    @JsonCreator
    public MenuName {
        validate(value);
    }

    private void validate(String value) {
        if (value == null || value.length() < MENU_NAME_MIN_LENGTH || value.length() > MENU_NAME_MAX_LENGTH) {
            throw new BadDataLengthException("메뉴 이름", MENU_NAME_MIN_LENGTH, MENU_NAME_MAX_LENGTH);
        }
    }
}
