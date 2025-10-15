package jnu.ie.capstone.menu.model.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jnu.ie.capstone.common.model.entity.BaseEntity;
import jnu.ie.capstone.menu.model.vo.MenuName;
import jnu.ie.capstone.menu.model.vo.Price;
import jnu.ie.capstone.store.model.entity.Store;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Setter
    @AttributeOverride(name = "value", column = @Column(name = "price", nullable = false))
    private Price price;

    @Setter
    @AttributeOverride(name = "value", column = @Column(name = "name", nullable = false))
    private MenuName name;

    @Setter
    @Column(nullable = false)
    @Array(length = 768)
    @JdbcTypeCode(SqlTypes.VECTOR)
    private float[] embedding;

    @Builder
    public Menu(Store store, MenuName name, Price price, float[] embedding) {
        this.store = store;
        this.name = name;
        this.price = price;
        this.embedding = embedding;
    }

}
