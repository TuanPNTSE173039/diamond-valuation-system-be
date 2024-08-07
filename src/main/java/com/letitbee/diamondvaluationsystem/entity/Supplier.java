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
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(columnDefinition = "text")
    private String image;
    @Column(columnDefinition = "nvarchar(50)")
    private String name;
    @Column(columnDefinition = "text")
    private String link;
    @OneToMany(mappedBy = "supplier",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private Set<DiamondMarket> diamondMarket = new HashSet<>();

}
