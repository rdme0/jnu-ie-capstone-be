package jnu.ie.capstone.store.model.entity;

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
import jnu.ie.capstone.member.model.entity.Member;
import jnu.ie.capstone.store.model.vo.StoreName;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Member owner;

    @AttributeOverride(name = "value", column = @Column(name = "name", nullable = false))
    private StoreName name;

    @Builder
    public Store(Member owner, StoreName name) {
        this.owner = owner;
        this.name = name;
    }

}