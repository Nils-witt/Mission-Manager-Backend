package dev.nilswitt.mission_manager.data.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Assignment of a {@link Qualification} to a {@link User}, carrying the per-assignment details
 * (when it was earned, when it expires, and an optional scanned certificate) that a plain
 * many-to-many relationship couldn't hold.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
public class UserQualification extends AbstractEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "qualification_id", nullable = false)
    private Qualification qualification;

    @NotNull
    @Column(nullable = false)
    private LocalDate since;

    @Column
    private LocalDate expiry;

    @Column(name = "certificate_stored_filename")
    private String certificateStoredFilename;

    @Column(name = "certificate_original_filename")
    private String certificateOriginalFilename;

    @Column(name = "certificate_content_type")
    private String certificateContentType;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public UserQualification(User user, Qualification qualification, LocalDate since, LocalDate expiry) {
        this.user = user;
        this.qualification = qualification;
        this.since = since;
        this.expiry = expiry;
    }

    public boolean hasCertificate() {
        return certificateStoredFilename != null;
    }

    public boolean isExpired() {
        return expiry != null && expiry.isBefore(LocalDate.now());
    }
}
