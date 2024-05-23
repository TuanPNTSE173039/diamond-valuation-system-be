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
    private long id;
    @Column(columnDefinition = "nvarchar(50)")
    private String firstName;
    @Column(columnDefinition = "nvarchar(50)")
    private String lastName;
    @Column(columnDefinition = "varchar(100)")
    private String email;
    @Column(columnDefinition = "char(20)")
    private String phone;
    @Column(columnDefinition = "int", nullable = false)
    private int experience;
    @Column(columnDefinition = "varchar(1000)")
    private String certificateLink;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @OneToMany(
            mappedBy = "staff",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<ValuationRequest> valuationRequestSet = new HashSet<>();
}
