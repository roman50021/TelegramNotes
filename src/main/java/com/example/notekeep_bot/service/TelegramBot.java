package com.example.notekeep_bot.service;

import com.example.notekeep_bot.config.BotConfig;
import com.example.notekeep_bot.model.*;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.mutation.internal.UpdateHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.*;

import static com.example.notekeep_bot.model.Note.titleNote;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    @Autowired
    BotConfig config;
    private UserRepository userRepository;
    private NoteRepository noteRepository;
    private ReminderRepository reminderRepository;

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Autowired
    public void setNoteRepository(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    @Autowired
    private void  setReminderRepository(ReminderRepository reminderRepository){
        this.reminderRepository = reminderRepository;
    }


    public TelegramBot(BotConfig config){
        this.config = config;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }
    @Override
    public String getBotToken() {
        return config.getToken();
    }
    private Long waitingNoteChatId = null;
    private Long waitingOutNote = null;

    private Long waitingNoteEditChatId = null;
    private String waitingNoteEditTitle = null;

    private Long waitingNoteDelete = null;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start":

                    start(chatId);
                    registerUser(update.getMessage());

                    break;
                case "Create note ✏\uFE0F":
                    sendMessage(chatId, "Напиши сюда свою заметку: ");
                    // Устанавливаем chatId в качестве ожидающего ввода заметки
                    waitingNoteChatId = chatId;
                    break;
                case "My notes \uD83D\uDCC1":

                    sendNoteKeyboard(chatId);

                    break;

                case "Back ↩":
                    sendMessage(chatId, "Меню");
                    break;

                case "Edit notes \uD83D\uDCDD":

                    sendMessage(chatId, "Отправь новое содержимое заметки!\nВот ваша старая заметка\n⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇⬇ ");
                    sendNoteKeyboard(chatId);
                    waitingNoteEditChatId  = chatId;

                    break;
                case "Delete note \uD83D\uDDD1":
                    sendNoteKeyboard(chatId);
                    waitingNoteDelete = chatId;

                    break;
                case "Reminders \uD83D\uDD14":
                    sendReminderKeyboard(chatId);

                    break;

                case "Create a reminder \uD83D\uDD14":
                    sendCreateReminderboard(chatId);

                    break;
                default:
                    if (waitingNoteDelete != null && waitingNoteDelete == chatId) {
                        String messageDelete = messageText;
                        noteDelete(chatId, messageDelete);
                        waitingNoteDelete = null;
                        break;
                    }
                    if (waitingNoteEditChatId  != null && waitingNoteEditChatId  == chatId) {
                        if(waitingNoteEditTitle == null){
                            waitingNoteEditTitle = messageText;

                        }else {
                            String newContext = messageText;
                            noteEdit(chatId, waitingNoteEditTitle, newContext);
                            waitingNoteChatId = null;
                            waitingNoteEditTitle = null;
                            break;
                        }
                    }
                    if (waitingOutNote != null && waitingOutNote == chatId) {
                        // Если да, то это введенная заметка
                        String messageOut = messageText;
                        noteOut(chatId, messageOut);
                        // Сбрасываем состояние ожидания ввода заметки
                        waitingOutNote = null;
                        break;
                    }

                    // Проверяем, находится ли чат в состоянии ожидания ввода заметки
                    if (waitingNoteChatId != null && waitingNoteChatId == chatId) {
                        // Если да, то это введенная заметка
                        String messageNote = messageText;
                        creatNote(chatId, messageNote);
                        // Сбрасываем состояние ожидания ввода заметки
                        waitingNoteChatId = null;
                        break;
                    }
                    else {
                        noteOut(chatId, messageText);
                        break;
                    }

            }

        }

    }
    private void noteEdit(long chatId, String title, String newContext){
        List<Note> notes = noteRepository.findByUser_ChatId(chatId);
        Note foundNote = null;

        for (Note note : notes) {
            if (title.equals(note.getTitle())) {
                foundNote = note;
                break;
            }
        }

        if (foundNote != null) {
            foundNote.setContext(newContext);
            foundNote.setTitle(titleNote(newContext));
            foundNote.setCreatedAt(new Timestamp((System.currentTimeMillis())));
            noteRepository.save(foundNote);
            sendMessage(chatId, "Заметка обновлина успешно! ✅");

        }
    }

    private void noteDelete(long chatId, String message){
        List<Note> notes = noteRepository.findByUser_ChatId(chatId);
        Note foundNote = null;
        for(Note note : notes){
            if(message.equals(note.getTitle())){
                foundNote = note;
            }
        }
        if(foundNote != null){
            noteRepository.deleteById(foundNote.getId());
            sendMessage(chatId, "Заметка удалена успешно! ✅");
        }
    }

    private void noteOut(long chatId, String message) {
        List<Note> notes = noteRepository.findByUser_ChatId(chatId);
        boolean noteFound = false;

        for (Note note : notes) {
            if (message.equals(note.getTitle())) {
                sendMessage(chatId, note.getContext());
                sendNoteKeyboard(chatId);
                noteFound = true;
                break;
            }
        }

    }
    private void sendNoteKeyboard(long chatId) {
        List<Note> notes = noteRepository.findByUser_ChatId(chatId);
        if (notes.isEmpty()) {
            sendMessage(chatId, "У вас нет сохраненных заметок. ❌");
        } else {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("Выберите заметку:");

            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            List<KeyboardRow> keyboardRows = new ArrayList<>();


            for (Note note : notes) {
                KeyboardRow row = new KeyboardRow();
                row.add(note.getTitle());
                keyboardRows.add(row);
            }

            KeyboardRow row = new KeyboardRow();
            row.add("Back ↩");
            keyboardRows.add(row);

            keyboardMarkup.setKeyboard(keyboardRows);
            message.setReplyMarkup(keyboardMarkup);

            try {
                execute(message);

            } catch (TelegramApiException e) {
                log.error("Ошибка при отправке сообщения: {}", e.getMessage());
            }
        }
    }


    private void creatNote(long chatId, String messageNote) {

        User user = userRepository.findByChatId(chatId);
        if (user == null) {
            sendMessage(chatId, "Пользователь не найден!");
            return;
        }
            Note note = new Note();
            note.setUser(user);
            note.setTitle(titleNote(messageNote));
            note.setContext(messageNote);
            note.setCreatedAt(new Timestamp(System.currentTimeMillis()));


            noteRepository.save(note);

            sendMessage(chatId, "Заметка сохранена успешно! ✅");


    }

    public void registerUser(Message msg) {
        if (userRepository.findById(msg.getChatId()).isEmpty()) {
            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();

            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("user saved: " + user);
        }
    }

    private void start(long chatId){
         String answer = "Привет! Я бот помошник! Я могу создавать и сохранять заметки.";
         sendMessage(chatId, answer);
    }
    private void sendMessage(long chatId, String textToSend){
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        keyboardMain(message);
        try{
            execute(message);
        }catch (TelegramApiException e){

        }
    }
    private void keyboardMain(SendMessage message){
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Create note ✏\uFE0F");
        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("My notes \uD83D\uDCC1");
        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("Edit notes \uD83D\uDCDD");
        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("Delete note \uD83D\uDDD1");
        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("Reminders \uD83D\uDD14");
        keyboardRows.add(row);


        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);
    }

    private void sendReminderKeyboard(long chatId) {
        List<Reminder> reminders = reminderRepository.findByUserChatId(chatId);

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("Напоминания :");

            ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
            List<KeyboardRow> keyboardRows = new ArrayList<>();

            KeyboardRow row = new KeyboardRow();
            row.add("Create a reminder \uD83D\uDD14");
            keyboardRows.add(row);


            for (Reminder reminder : reminders) {
                row = new KeyboardRow();
                row.add(reminder.getText());
                keyboardRows.add(row);
            }

            row = new KeyboardRow();
            row.add("Back ↩");
            keyboardRows.add(row);

            keyboardMarkup.setKeyboard(keyboardRows);
            message.setReplyMarkup(keyboardMarkup);

            try {
                execute(message);

            } catch (TelegramApiException e) {
                log.error("Ошибка при отправке сообщения: {}", e.getMessage());
            }
    }

    private void sendCreateReminderboard(long chatId) {
        List<Reminder> reminders = reminderRepository.findByUserChatId(chatId);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Когда ставим напоминание ?");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Через час  ⏰");
        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("Завтра утром 8:00 \uD83D\uDD57");
        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("Завтра днём 12:00 \uD83D\uDD5B");
        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("Завтра вечером 17:00 \uD83D\uDD54");
        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("Послезавтра утром 8:00 \uD83D\uDD57");
        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("Послезавтра днём 12:00 \uD83D\uDD5B");
        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("Послезавтра вечером 17:00 \uD83D\uDD54");
        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("Back ↩");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения: {}", e.getMessage());
        }
    }




}
