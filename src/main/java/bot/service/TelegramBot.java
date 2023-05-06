package bot.service;

import bot.config.BotConfig;
import bot.database.PersonDAO;
import bot.models.Person;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideoNote;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@Controller
public class TelegramBot extends TelegramLongPollingBot {
    final BotConfig config;
    private PersonDAO personDAO;

    //Stare 0 - Регистрация
    // 1 - Подтверждение
    // 2- Основное меню
    // 3 - Пополнение баланса

    private int state = 0;
    private double money = 0;

    @Autowired
    public TelegramBot(BotConfig config, PersonDAO personDAO) {
        this.config = config;
        this.personDAO = personDAO;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }
    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        long chatId = 0;
        if(update.hasMessage()){
            chatId = update.getMessage().getChatId();
        }else{
            chatId = update.getCallbackQuery().getMessage().getChatId();
        }
        long admin = 449446599L;
        //Создаю заранее все поля которые буду отправлять пользователю!
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        SendVideoNote sendVideoNote = new SendVideoNote();
        sendVideoNote.setChatId(chatId);


        Person person = personDAO.show(chatId);
        //Пользователь новый в системе!
        if(person == null) {
            if (update.hasMessage() && update.getMessage().hasText()) {

                String message = update.getMessage().getText();
                if (message.equals("/start")) {
                    sendMessage.setText("Howdy!, я телеграмм бот по аренде автомобилей\n" +
                            "Предлагаю тебе пройти авторизацию для начала!\n/confirm");
                    execute(sendMessage);
                }
                if (message.equals("/confirm")) {
                    sendMessage.setText("Для регистрации напишите кто вы, менеджер или покупатель!\n" +
                            "Придумайте пароль и логин в формате username:Логин password:Пароль email:почта");
                    execute(sendMessage);
                }
                if (message.contains("username:") && message.contains("password:")) {
                    String mes = message.replace("username:", "").replace("password:", "")
                            .replace("email:", "");
                    sendMessage.setText(mes);
                    execute(sendMessage);
                    String[] arr = mes.split(" ");
                    person = new Person();
                    person.setChatId(chatId);
                    person.setUsername(arr[0]);
                    person.setPassword(arr[1]);
                    person.setEmail(arr[2]);
                    person.setBalance(0.0);
                    person.setState(1);
                    personDAO.save(person);
                }
            }
        }
        if(person.getState() == 1) {
                if(update.getMessage().hasPhoto()){
                    List<PhotoSize> photos = update.getMessage().getPhoto();
                    PhotoSize photo = photos.stream()
                            .sorted(Comparator.comparing(PhotoSize::getFileSize).reversed())
                            .findFirst()
                            .orElse(null);
                    InputFile file = new InputFile(photo.getFileId());


                    if(photo!= null){
                        sendPhoto.setChatId(admin);
                        sendPhoto.setPhoto(file);
                        sendMessage.setText("Даниил Андреевич, проверте фото отправленное клиентом для подтверждения личности!" +
                                "\nДля подтверждения напишите нажмите на \n/Confirm_"+chatId+"\nИли\n/Decline_"+chatId);
                        sendMessage.setChatId(admin);
                        execute(sendMessage);
                        execute(sendPhoto);
                    }



                }else{
                    sendMessage.setText(update.getMessage().getFrom().getUserName()+", отправить свои паспортные данные и водительское удостоверение для подтверждения личности!");
                    execute(sendMessage);
                }
        }

        //Главное меню
        if(person.getState() == 2){
            System.out.println("2");
            if(update.getMessage() != null) {
                InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
                List<InlineKeyboardButton> rowInLine = new ArrayList<>();

                InlineKeyboardButton profile = new InlineKeyboardButton();
                InlineKeyboardButton balance = new InlineKeyboardButton();
                InlineKeyboardButton cars = new InlineKeyboardButton();

                profile.setText("Профиль");
                profile.setCallbackData("profile");

                balance.setText("Баланс");
                balance.setCallbackData("balance");

                cars.setText("Машины");
                cars.setCallbackData("cars");

                rowInLine.add(profile);
                rowInLine.add(balance);
                rowInLine.add(cars);

                rowsInLine.add(rowInLine);

                markupInLine.setKeyboard(rowsInLine);
                SendMessage newMessage = new SendMessage();
                newMessage.setChatId(chatId);
                newMessage.setText("Привет, " + update.getMessage().getChat().getUserName() + "" +
                        "\nТы находишься в главном меню!" +
                        "\nВыбери действие!");
                newMessage.setReplyMarkup(markupInLine);
                execute(newMessage);
            } else if (update.hasCallbackQuery()) {

                String callBackData = update.getCallbackQuery().getData();
                long messageId = update.getCallbackQuery().getMessage().getMessageId();
                long chatIdCallBack = update.getCallbackQuery().getMessage().getChatId();

                if(callBackData.equals("profile")){
                    ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
                    List<KeyboardRow> rows = new ArrayList<>();
                    KeyboardRow row = new KeyboardRow();
                    row.add("Назад");
                    rows.add(row);
                    markup.setKeyboard(rows);
                    markup.setOneTimeKeyboard(true);
                    person = personDAO.show(chatId);
                    String text = "Ваш никнейм: "+person.getUsername()
                            +"\nВаш баланс: "+person.getBalance()+" рублей";
                    person.setState(3);
                    personDAO.update(person.getId(),person);
                    EditMessageText messageText = new EditMessageText();
                    messageText.setChatId(chatIdCallBack);
                    messageText.setText(text);
                    messageText.setMessageId((int) messageId);
                    sendMessage.setText("Чтобы вернуться назад нажмите на кнопку!");
                    sendMessage.setReplyMarkup(markup);
                    execute(messageText);
                    execute(sendMessage);

                } else if (callBackData.equals("balance")) {
                    ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
                    List<KeyboardRow> rows = new ArrayList<>();
                    KeyboardRow row = new KeyboardRow();
                    row.add("Назад");
                    row.add("Пополнить баланс");
                    rows.add(row);
                    markup.setResizeKeyboard(true);
                    markup.setKeyboard(rows);
                    markup.setOneTimeKeyboard(true);
                    person = personDAO.show(chatId);
                    String text ="Ваш баланс: "+person.getBalance()+" рублей" +
                            "\nВы можете пополнить баланс нажав на кнопку пополнить баланс" +
                            "\nМинимальная сумма пополнения 100 рублей!";
                    person.setState(4);
                    personDAO.update(person.getId(),person);
                    EditMessageText messageText = new EditMessageText();
                    messageText.setChatId(chatIdCallBack);
                    messageText.setText(text);
                    messageText.setMessageId((int) messageId);
                    sendMessage.setText("Выберите действие");
                    sendMessage.setReplyMarkup(markup);
                    execute(messageText);
                    execute(sendMessage);
                } else if (callBackData.equals("cars")) {
                }
            }


        }

        //Вернуться назад из профиля и баланса
        if(person.getState() == 3){
            if(update.getMessage() != null) {
                if (update.getMessage().getText().equals("Назад")) {
                    sendMessage.setText("Вы были перенаправлены в меню, для его появления напишите любой текст!");
                    person.setState(2);
                    personDAO.update(person.getId(), person);
                    execute(sendMessage);
                }
            }
        }

        //Находимся в балансе
        if(person.getState() == 4){


                if(update.getMessage() != null) {
                    if (!update.getMessage().getText().equals("Пополнить баланс")) {
                        try {
                            String message = update.getMessage().getText();
                            money = Double.parseDouble(message);
                            money = money + money * 0.05;
                            InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
                            List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
                            List<InlineKeyboardButton> rowInLine = new ArrayList<>();

                            InlineKeyboardButton balance = new InlineKeyboardButton();


                            balance.setText("Я пополнил");
                            balance.setCallbackData("balance_add");


                            rowInLine.add(balance);


                            rowsInLine.add(rowInLine);

                            markupInLine.setKeyboard(rowsInLine);
                            SendMessage newMessage = new SendMessage();
                            newMessage.setChatId(chatId);
                            newMessage.setText("Вы создали счет на пополнение\n" +
                                    "Сумма пополнения: " + money);
                            newMessage.setReplyMarkup(markupInLine);
                            execute(newMessage);
                        } catch (NumberFormatException e) {
                            sendMessage.setText("Вы ввели некорректный формат пополнения!");
                            execute(sendMessage);
                        }


                    }else
                    if(update.getMessage().getText().equals("Назад")){
                        sendMessage.setText("Вы были перенаправлены в меню, для его появления напишите любой текст!");
                        person.setState(2);
                        personDAO.update(person.getId(), person);
                        execute(sendMessage);
                    }else
                    if(update.getMessage().getText().equals("Пополнить баланс")){
                        sendMessage.setText("Введите сумму пополнения, просим обратить внимание" +
                                "\nЧто при пополнение будет взыскана коммиссия 5% от пополнения");
                        execute(sendMessage);
                    }
                }else if (update.hasCallbackQuery()) {

                    String callBackData = update.getCallbackQuery().getData();
                    long messageId = update.getCallbackQuery().getMessage().getMessageId();
                    long chatIdCallBack = update.getCallbackQuery().getMessage().getChatId();

                    if (callBackData.equals("balance_add")) {

                        person.setBalance(person.getBalance() + money);
                        String text = "Ваш  баланс пополнен!";
                        person.setState(2);
                        personDAO.update(person.getId(), person);
                        EditMessageText messageText = new EditMessageText();
                        messageText.setChatId(chatIdCallBack);
                        messageText.setText(text);
                        messageText.setMessageId((int) messageId);
                        execute(messageText);
                    }
                }


        }

        //ADMIN!!!
        if(chatId == admin){
            if(update.getMessage() != null){
                if(update.getMessage().getText().contains("/Confirm")) {
                    String adminMessage = update.getMessage().getText();
                    String[] arr = adminMessage.split("_");
                    long userId = Long.parseLong(arr[1]);
                    sendMessage.setText("Вас подтвердил администратор!");
                    person = personDAO.show(userId);
                    person.setConfirmed(true);
                    person.setState(2);
                    personDAO.update(person.getId(), person);
                    execute(sendMessage);
                } else if (update.getMessage().getText().contains("/Decline")) {
                    String adminMessage = update.getMessage().getText();
                    String[] arr = adminMessage.split("_");
                    long userId = Long.parseLong(arr[1]);
                    sendMessage.setText("Администратор отказал вам в подтверждении!\nДля уточнение напишите ему:\n" +
                            "@waitoneminute");
                    execute(sendMessage);
                }
            }
        }
    }





}
