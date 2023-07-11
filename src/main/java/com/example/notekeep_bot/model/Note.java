package com.example.notekeep_bot.model;

import jakarta.persistence.*;
import org.w3c.dom.Text;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Objects;

@Entity(name = "notesDataTable")
public class Note {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private User user;


    private String title;


    private String context;

    private Timestamp createdAt;

    public static String titleNote(String context) {
        ArrayList<Character> charList = new ArrayList<>();
        for (char ch : context.toCharArray()) {
            charList.add(ch);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 15 && i < charList.size(); i++) {
            sb.append(charList.get(i));
        }

        return sb.toString() + " ...";
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note note = (Note) o;
        return Objects.equals(title, note.title) && Objects.equals(context, note.context);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, context);
    }
}