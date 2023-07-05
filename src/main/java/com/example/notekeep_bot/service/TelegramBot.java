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
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



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

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            boolean creatingNote = false; // Флаг, указывающий на процесс создания заметки

            if (messageText.equals("/start")) {
                start(chatId);
            } else if (messageText.equals("Create note")) {
                createNote(chatId);
                creatingNote = true; // Устанавливаем флаг, чтобы указать на начало создания заметки
            } else if (creatingNote == true) {
                processNoteTitle(chatId, messageText);
                creatingNote = true; // Сбрасываем флаг после создания заметки
            } else if (creatingNote == true){
                processNoteContent(chatId, messageText);
                // Добавьте обработку неизвестной команды или сообщения
            }
        }
    }





    /////////////////////////////////  \/Создание заметки\/  ///////////////////////////////////////////////
    private void createNote(long chatId) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(String.valueOf(chatId));
        sendMessageRequest.setText("Введите заголовок заметки:");

        try {
            execute(sendMessageRequest);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения пользователю: {}", e.getMessage());
        }
    }

    // Добавьте этот метод для обработки полученных данных от пользователя
    private void processNoteTitle(long chatId, String title) {
        User user = userRepository.findByChatId(chatId);
        if (user != null) {
            Note note = new Note();
            note.setUser(user);
            note.setTitle(title);
            note.setCreatedAt(new Timestamp(System.currentTimeMillis()));

            // Сохранение заметки в базе данных
            noteRepository.save(note);

            SendMessage sendMessageRequest = new SendMessage();
            sendMessageRequest.setChatId(String.valueOf(chatId));
            sendMessageRequest.setText("Введите содержимое заметки:");

            try {
                execute(sendMessageRequest);
            } catch (TelegramApiException e) {
                log.error("Ошибка при отправке сообщения пользователю: {}", e.getMessage());
            }
        } else {
            SendMessage sendMessageRequest = new SendMessage();
            sendMessageRequest.setChatId(String.valueOf(chatId));
            sendMessageRequest.setText("Пользователь не найден. Пожалуйста, используйте команду /start для начала.");

            try {
                execute(sendMessageRequest);
            } catch (TelegramApiException e) {
                log.error("Ошибка при отправке сообщения пользователю: {}", e.getMessage());
            }
        }
    }

    // Добавьте этот метод для обработки полученных данных от пользователя
    private void processNoteContent(long chatId, String content) {
        User user = userRepository.findByChatId(chatId);
        if (user != null) {
            Note note = noteRepository.findByUserId(user.getChatId());
            if (note != null) {
                note.setContext(content);
                // Обновление заметки в базе данных
                noteRepository.save(note);

                SendMessage sendMessageRequest = new SendMessage();
                sendMessageRequest.setChatId(String.valueOf(chatId));
                sendMessageRequest.setText("Заметка успешно создана!");

                try {
                    execute(sendMessageRequest);
                } catch (TelegramApiException e) {
                    log.error("Ошибка при отправке сообщения пользователю: {}", e.getMessage());
                }
            } else {
                SendMessage sendMessageRequest = new SendMessage();
                sendMessageRequest.setChatId(String.valueOf(chatId));
                sendMessageRequest.setText("Заметка не найдена. Пожалуйста, используйте команду /start и создайте новую заметку.");

                try {
                    execute(sendMessageRequest);
                } catch (TelegramApiException e) {
                    log.error("Ошибка при отправке сообщения пользователю: {}", e.getMessage());
                }
            }
        } else {
            SendMessage sendMessageRequest = new SendMessage();
            sendMessageRequest.setChatId(String.valueOf(chatId));
            sendMessageRequest.setText("Пользователь не найден. Пожалуйста, используйте команду /start для начала.");

            try {
                execute(sendMessageRequest);
            } catch (TelegramApiException e) {
                log.error("Ошибка при отправке сообщения пользователю: {}", e.getMessage());
            }
        }
    }




    ////////////////////////////////   /\Создание заметки/\   //////////////////////////////////////////////


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
        row.add("List of notes");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);
    }




}
