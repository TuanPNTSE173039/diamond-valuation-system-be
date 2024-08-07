package com.letitbee.diamondvaluationsystem.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table
public class Staff {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;
    @Column(columnDefinition = "nvarchar(50)", nullable = false)
    private String firstName;
    @Column(columnDefinition = "nvarchar(50)", nullable = false)
    private String lastName;
    @Column(columnDefinition = "char(10)", nullable = false, unique = true)
    private String phone;
    @Column(columnDefinition = "int", nullable = false)
    private int experience;
    @Column(columnDefinition = "varchar(1000)")
    private String certificateLink;
    @Column(columnDefinition = "text")
    private String avatar;

    @OneToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @OneToMany(
            mappedBy = "staff",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<ValuationRequest> valuationRequestSet = new HashSet<>();

    @OneToMany(mappedBy = "staff", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DiamondValuationAssign> diamondValuationAssigns = new HashSet<>();
}
