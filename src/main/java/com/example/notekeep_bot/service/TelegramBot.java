package com.example.notekeep_bot.service;

import com.example.notekeep_bot.config.BotConfig;
import com.example.notekeep_bot.model.Note;
import com.example.notekeep_bot.model.NoteRepository;
import com.example.notekeep_bot.model.User;
import com.example.notekeep_bot.model.UserRepository;
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

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Autowired
    public void setNoteRepository(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
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
                case "Create note":
                    sendMessage(chatId, "Напиши сюда свою заметку: ");
                    // Устанавливаем chatId в качестве ожидающего ввода заметки
                    waitingNoteChatId = chatId;
                    break;
                case "My notes":
                    sendNoteKeyboard(chatId);

                    break;

                case "Back":
                    sendMessage(chatId, "Меню");
                    break;

                case "Edit note":

                    break;
                case "Delete note":

                    break;
                default:
                    if (waitingOutNote != null && waitingOutNote == chatId) {
                        // Если да, то это введенная заметка
                        String messageOut = messageText;
                        noteOut(chatId, messageOut);
                        // Сбрасываем состояние ожидания ввода заметки
                        waitingOutNote = null;
                    }

                    // Проверяем, находится ли чат в состоянии ожидания ввода заметки
                    if (waitingNoteChatId != null && waitingNoteChatId == chatId) {
                        // Если да, то это введенная заметка
                        String messageNote = messageText;
                        creatNote(chatId, messageNote);
                        // Сбрасываем состояние ожидания ввода заметки
                        waitingNoteChatId = null;
                    }
                    else {
                        noteOut(chatId, messageText);
                    }
                    break;
            }
        }
    }
    private void noteOut(long chatId, String message) {
        List<Note> notes = noteRepository.findByUser_ChatId(chatId);
        boolean noteFound = false;

        for (Note note : notes) {
            if (message.equals(note.getTitle())) {
                sendMessage(chatId, note.getContext());
                noteFound = true;
                break;
            }
        }

        if (!noteFound) {
            sendMessage(chatId, "Такой заметки не существует");
        }
    }
    private void sendNoteKeyboard(long chatId) {
        List<Note> notes = noteRepository.findByUser_ChatId(chatId);
        if (notes.isEmpty()) {
            sendMessage(chatId, "У вас нет сохраненных заметок.");
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
            row.add("Back");
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

        sendMessage(chatId, "Заметка сохранена успешно!");
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
        row.add("Create note");
        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("My notes");
        keyboardRows.add(row);


        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);
    }

}
