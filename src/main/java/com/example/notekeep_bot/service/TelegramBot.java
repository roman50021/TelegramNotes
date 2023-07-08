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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private Long waitingEditNoteId = null;

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
                case "List of notes":
                    listNoteWithKeyboard(chatId);


                    break;
                case "Edit note":
                    sendMessage(chatId, "Напиши сюда свой номер заметки которую хочешь редактировать: ");
                    listNote(chatId);
                    //waitingEditNoteId = noteRepository.findById();

                    break;
                case "Delete note":

                    break;
                default:
                    // Проверяем, находится ли чат в состоянии ожидания ввода заметки
                    if (waitingNoteChatId != null && waitingNoteChatId == chatId) {
                        // Если да, то это введенная заметка
                        String messageNote = messageText;
                        creatNote(chatId, messageNote);
                        // Сбрасываем состояние ожидания ввода заметки
                        waitingNoteChatId = null;
                    }
                    break;
            }
        }
    }

    private void listNoteWithKeyboard(long chatId) {
        List<Note> notes = noteRepository.findByUser_ChatId(chatId);
        if (notes.isEmpty()) {
            sendMessage(chatId, "У вас нет сохраненных заметок.");
        } else {
            StringBuilder noteList = new StringBuilder("Ваши заметки:\n");
            for (Note note : notes) {
                noteList.append(note.getId()).append(" - ").append(titleNote(note.getContext())).append("\n");
            }

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Получить полный текст");
            button.setCallbackData("get_full_text");

            List<List<InlineKeyboardButton>> keyboardRows = new ArrayList<>();
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboardRows.add(row);

            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup(keyboardRows);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(noteList.toString());
            message.setReplyMarkup(keyboardMarkup);

            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private void listNote(long chatId){
        List<Note> notes = noteRepository.findByUser_ChatId(chatId);
        if(notes.isEmpty()){
            sendMessage(chatId, "У вас нет сохраненных заметок.");
        } else {
            StringBuilder noteList = new StringBuilder("Ваши заметки:\n");
            for (Note note : notes) {
                noteList.append(note.getId()).append(" - ").append(titleNote(note.getContext())).append("\n");
            }

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Получить полный текст");
            button.setCallbackData("get_full_text");

            // Создание списка кнопок
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboard.add(row);

            // Создание объекта InlineKeyboardMarkup и установка списка кнопок
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup(keyboard);

            // Создание сообщения с клавиатурой
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Список заметок:");

            // Отправка сообщения с клавиатурой
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
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

    private void sendMessage(long chatId, String textToSend,  InlineKeyboardMarkup markup){
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        message.setText(String.valueOf(markup));
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
        row = new KeyboardRow();
        row.add("Edit note");
        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("Delete note");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);
    }


    //
//    private void sendMessageNote(long chatId){
//        SendMessage message = new SendMessage();
//        message.setChatId(String.valueOf(chatId));
//        keyboardNote(chatId, message);
//        try{
//            execute(message);
//        }catch (TelegramApiException e){
//
//        }
//    }
//
//    private void keyboardNote(long chatId, SendMessage message) {
//        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
//        List<KeyboardRow> keyboardRows = new ArrayList<>();
//
//        List<Note> notes = noteRepository.findByUser_ChatId(chatId);
//        for (Note note : notes) {
//            KeyboardRow row = new KeyboardRow();
//            row.add(Note.titleNote(note.getContext())); // Используйте метод titleNote() для получения первых 20 символов контекста заметки
//            keyboardRows.add(row);
//        }
//
//        // Добавление кнопок "Edit note" и "Delete note"
//        KeyboardRow editRow = new KeyboardRow();
//        editRow.add("Edit note");
//        keyboardRows.add(editRow);
//
//        KeyboardRow deleteRow = new KeyboardRow();
//        deleteRow.add("Delete note");
//        keyboardRows.add(deleteRow);
//
//        keyboardMarkup.setKeyboard(keyboardRows);
//        message.setReplyMarkup(keyboardMarkup);
//
//
//    }





}
