package com.dentruth.consultprepare.domain.entity;

import com.dentruth.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsultRecommendedQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long consultPrepareId;

    @Column(nullable = false)
    private Integer questionOrder;

    @Column(nullable = false, length = 300)
    private String question;

    public ConsultRecommendedQuestion(
            Long consultPrepareId,
            Integer questionOrder,
            String question
    ) {
        this.consultPrepareId = consultPrepareId;
        this.questionOrder = questionOrder;
        this.question = question;
    }

}
