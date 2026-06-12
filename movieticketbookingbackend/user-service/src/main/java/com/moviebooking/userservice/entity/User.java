package com.moviebooking.userservice.entity;

import com.moviebooking.userservice.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 60)
    private String password;

    @Column(length = 15)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default // ← Ensures Builder sets this default even when not specified
    private Role role = Role.ROLE_USER; // New users are always ROLE_USER by default

    /**
     * Automatically set by Hibernate on first INSERT.
     * 
     * @CreationTimestamp: Hibernate fills this before the INSERT query runs.
     *                     updatable=false: Once set, this column is never changed
     *                     on UPDATE.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Automatically updated by Hibernate on every UPDATE.
     * 
     * @UpdateTimestamp: Hibernate refreshes this on every save/merge.
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
