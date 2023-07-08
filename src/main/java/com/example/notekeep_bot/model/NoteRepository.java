package com.example.notekeep_bot.model;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteRepository extends CrudRepository<Note, Long> {
    @Query("SELECT n FROM notesDataTable n WHERE n.user.chatId = :chatId")
    List<Note> findByUser_ChatId(@Param("chatId") long chatId);

    Note findNoteById(Long Id);
}
