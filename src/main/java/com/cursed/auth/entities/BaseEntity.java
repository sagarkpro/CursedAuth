package com.cursed.auth.entities;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode
public class BaseEntity {
    @Id
    @Builder.Default
    String id = UUID.randomUUID().toString();
    Instant createdAt;
    Instant updatedAt;
    String updatedBy;
}
