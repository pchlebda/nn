package com.nationale.currency.acoount;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
public class AccountEntity {

    @Id
    private String apiKey;
    @Column(nullable = false)
    private String firstName;
    @Column(nullable = false)
    private String lastName;
    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal balancePLN;
    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal balanceUSD;
}
