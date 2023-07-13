package com.example.notekeep_bot.model;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReminderRepository extends CrudRepository<Reminder, Long> {
    @Query("SELECT n FROM remindersDataTable n WHERE n.user.chatId = :chatId")
    List<Reminder> findByUserChatId(@Param("chatId") long chatId);
}
