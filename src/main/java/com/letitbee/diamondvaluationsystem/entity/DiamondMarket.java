package com.letitbee.diamondvaluationsystem.entity;

import com.letitbee.diamondvaluationsystem.enums.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table
public class DiamondMarket {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;
    @Column(columnDefinition = "varchar(1000)", nullable = false)
    private String diamondImage;
    @Column(columnDefinition = "datetime")
    private Date creationDate;
    @Column(columnDefinition = "varchar(100)")
    private String certificateId;
    @Column(nullable = false)
    private float caratWeight;
    @Enumerated(EnumType.STRING)
    private DiamondOrigin diamondOrigin;
    @Enumerated(EnumType.STRING)
    private Color color;
    @Enumerated(EnumType.STRING)
    private Cut cut;
    @Enumerated(EnumType.STRING)
    private Clarity clarity;
    @Enumerated(EnumType.STRING)
    private Polish polish;
    @Enumerated(EnumType.STRING)
    private Symmetry symmetry;
    @Enumerated(EnumType.STRING)
    private Shape shape;
    @Enumerated(EnumType.STRING)
    private Fluorescence fluorescence;
    @Column(nullable = false)
    private Double cutScore;
    @Column(columnDefinition = "money", nullable = false)
    private Double price;
    @Column
    private String link;

    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;
}
