package com.moviebooking.theatreservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Theatre entity — represents a cinema multiplex location.
 *
 * ═══════════════════════════════════════════════════════════
 * RELATIONSHIP: Theatre → Screen (One-to-Many)
 * ═══════════════════════════════════════════════════════════
 * One Theatre has multiple Screens (auditoriums).
 * PVR Cinemas Mumbai might have 8 screens; INOX Delhi might have 6.
 *
 * cascade = ALL: deleting a theatre deletes all its screens (and their seats).
 * orphanRemoval = true: removing a screen from this list deletes it from DB.
 * This makes Theatre the "aggregate root" — it controls the lifecycle of screens.
 *
 * FetchType.LAZY: We rarely need ALL screens when just listing theatres.
 * The detail endpoint loads screens explicitly when needed.
 */
@Entity
@Table(name = "theatres")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Theatre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;         // "PVR: Phoenix Palladium, Mumbai"

    @Column(nullable = false, length = 100)
    private String city;         // "Mumbai" — used for filtering by city

    @Column(nullable = false, length = 500)
    private String address;      // Full street address

    @Column(length = 10)
    private String pincode;

    @Column(length = 15)
    private String phone;

    @Column(length = 150)
    private String email;

    @OneToMany(mappedBy = "theatre", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Screen> screens = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** Convenience method — total screens derived from list size */
    public int getTotalScreens() {
        return screens != null ? screens.size() : 0;
    }
}
