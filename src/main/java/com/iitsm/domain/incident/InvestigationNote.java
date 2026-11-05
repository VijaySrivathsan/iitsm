package com.iitsm.domain.incident;

import com.iitsm.domain.BaseEntity;
import com.iitsm.domain.Checks;
import com.iitsm.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One entry in an incident's investigation log: a technician note, an information request, or the
 * reporter's reply. Owned by its {@link Incident} (composition): it is created through
 * {@link Incident#addNote} and deleted with the incident. Immutable once written.
 */
@Entity
@Table(name = "investigation_note")
public class InvestigationNote extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoteType type;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(nullable = false)
    private Instant createdAt;

    protected InvestigationNote() {
    }

    InvestigationNote(User author, NoteType type, String text, Instant createdAt) {
        this.author = Checks.requireNonNull(author, "Note author");
        this.type = Checks.requireNonNull(type, "Note type");
        this.text = Checks.requireText(text, "Note text");
        this.createdAt = Checks.requireNonNull(createdAt, "Note time");
    }

    public User getAuthor() {
        return author;
    }

    public NoteType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
