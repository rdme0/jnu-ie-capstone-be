package jnu.ie.capstone.menu.model.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jnu.ie.capstone.menu.model.vo.OptionName;
import jnu.ie.capstone.menu.model.vo.Price;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Option {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @AttributeOverride(name = "value", column = @Column(name = "name", nullable = false))
    private OptionName name;

    @AttributeOverride(name = "value", column = @Column(name = "price", nullable = false))
    private Price price;

    @Builder
    public Option(Menu menu, OptionName name, Price price) {
        this.menu = menu;
        this.name = name;
        this.price = price;
    }

}
