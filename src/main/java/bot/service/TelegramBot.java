package bot.service;

import bot.config.BotConfig;
import bot.database.CarsDAO;
import bot.database.PersonDAO;
import bot.models.Car;
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
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@Controller
public class TelegramBot extends TelegramLongPollingBot {
    final BotConfig config;
    private PersonDAO personDAO;
    private CarsDAO carDAO;
    private Car registrycar = new Car();
    private Person registryPerson = new Person();

    //Stare 0 - Регистрация
    // 1 - Подтверждение
    // 2- Основное меню
    // 3 - Пополнение баланса

    private double money = 0;
    private int state;

    @Autowired
    public TelegramBot(BotConfig config, PersonDAO personDAO,CarsDAO carDAO) {
        this.config = config;
        this.personDAO = personDAO;
        this.carDAO = carDAO;
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
                if (update.hasMessage()) {
                    String message = update.getMessage().getText();
                    if (message.contains("/start")) {
                        sendMessage.setText("Привет, я бот по аренде автомобилей!" +
                                "\nПока что я тебя не знаю, и поэтому тебе нужно подтвердить свои данные" +
                                "\nДавай займемся этим!" +
                                "\nЧтобы начать регистрацию введи свою роль client или manager");
                        execute(sendMessage);
                        state =2;
                    }else if(state ==2){
                        registryPerson.setRole(update.getMessage().getText());
                        registryPerson.setBalance(0.0);
                        registryPerson.setUsername(update.getMessage().getFrom().getUserName());
                        registryPerson.setCar(0);
                        registryPerson.setConfirmed(false);
                        registryPerson.setChatId(update.getMessage().getChatId());
                        sendMessage.setText("Введите свою почту!");
                        state =3;
                        execute(sendMessage);
                    }else if(state ==3){
                        registryPerson.setEmail(update.getMessage().getText());
                        sendMessage.setText("Введите пароль!");
                        execute(sendMessage);
                        state =4;
                    }else if(state ==4){
                        registryPerson.setPassword(update.getMessage().getText());
                        sendMessage.setText("Регистрация окончена, для продолжения отправьте свое фото с паспортом и лицом!");
                        execute(sendMessage);
                        registryPerson.setState(1);
                        personDAO.save(registryPerson);
                        state = 1337;
                    }

            }
        }else
        if(!person.isConfirmed() && person != null) {
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



                }else if(!update.getMessage().getText().contains("Confirm")){
                    sendMessage.setText(update.getMessage().getFrom().getUserName()+", отправить свои паспортные данные и водительское удостоверение для подтверждения личности!");
                    execute(sendMessage);
                }
        }

        //////////////////////////////////////////////////////////////
        //ГЛАВНОЕ МЕНЮ ПОЛЬЗОВАТЕЛЯ
        //////////////////////////////////////////////////////////////
        else if(person.getState() == 2 && person.getRole().equals("client")){
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
                    markup.setResizeKeyboard(true);
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
                            "\nВы можете пополнить баланс нажав на кнопку пополнить баланс";

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

                    person.setState(5);
                    personDAO.update(person.getId(),person);
                    ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
                    List<KeyboardRow> rows = new ArrayList<>();
                    KeyboardRow row = new KeyboardRow();
                    row.add("Назад");
                    row.add("Машины");
                    rows.add(row);
                    markup.setKeyboard(rows);
                    markup.setOneTimeKeyboard(true);
                    markup.setResizeKeyboard(true);

                    sendMessage.setReplyMarkup(markup);
                    sendMessage.setText("Вы находитесь в меню машин, введите комманду нажмите на кнопку на экране" +
                            "для прогружения списка доступных автомобилей");
                    execute(sendMessage);
                }
            }


        }

        //Вернуться назад из профиля и баланса
        else if(person.getState() == 3){
            if(update.getMessage() != null) {
                if (update.getMessage().getText().equals("Назад")) {
                    sendMessage.setText("Вы были перенаправлены в меню, для его появления нажмите на \n" +
                            "/menu");
                    person.setState(2);
                    personDAO.update(person.getId(), person);
                    execute(sendMessage);
                }
            }
        }
        //////////////////////////////////////////////////////////////
        //БАЛАНС для менеджера и клиента
        //////////////////////////////////////////////////////////////
        else if(person.getState() == 4){


                if(update.getMessage() != null) {
                    if(update.getMessage().getText().equals("Пополнить баланс")){
                        if(person.getRole().equals("client")) {
                            sendMessage.setText("Введите сумму пополнения, просим обратить внимание" +
                                    "\nЧто при пополнение будет взыскана коммиссия 5% от пополнения");
                        } else if (person.getRole().equals("manager")) {
                            sendMessage.setText("Менеджерам недоступно пополнение баланса!");
                        }
                        execute(sendMessage);
                    }else
                    if(update.getMessage().getText().equals("Назад")){
                        sendMessage.setText("Вы были перенаправлены в меню, для его появления воспользуйтесь коммандой" +
                                "\n /menu");
                        person.setState(2);
                        personDAO.update(person.getId(), person);
                        execute(sendMessage);
                    } else if (update.getMessage().getText().contains("Вывести деньги")) {

                        if(person.getRole().equals("manager")){
                            sendMessage.setText("Введите сумму которую хотите вывести");
                        } else if (person.getRole().equals("client")) {
                            sendMessage.setText("Клиентам недоступен вывод денег!");
                        }

                        execute(sendMessage);
                    } else
                    if (!update.getMessage().getText().equals("Пополнить баланс") &&
                    !update.getMessage().getText().equals("Назад")) {
                        try {
                            if(person.getRole().equals("client")) {
                                String message = update.getMessage().getText();
                                money = Double.parseDouble(message);
                                money = money - money * 0.05;
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
                            }else if(person.getRole().equals("manager")){
                                String message = update.getMessage().getText();
                                money = Double.parseDouble(message);
                                InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
                                List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
                                List<InlineKeyboardButton> rowInLine = new ArrayList<>();

                                InlineKeyboardButton balance = new InlineKeyboardButton();


                                balance.setText("Вывести");
                                balance.setCallbackData("balance_remove");


                                rowInLine.add(balance);


                                rowsInLine.add(rowInLine);

                                markupInLine.setKeyboard(rowsInLine);
                                SendMessage newMessage = new SendMessage();
                                newMessage.setChatId(chatId);
                                newMessage.setText("Вы создали счет на вывод\n" +
                                        "Сумма пополнения: " + money);
                                newMessage.setReplyMarkup(markupInLine);
                                execute(newMessage);
                            }
                        } catch (NumberFormatException e) {
                            sendMessage.setText("Вы ввели некорректный формат пополнения!");
                            execute(sendMessage);
                        }


                    }
                }else if (update.hasCallbackQuery()) {

                    String callBackData = update.getCallbackQuery().getData();
                    long messageId = update.getCallbackQuery().getMessage().getMessageId();
                    long chatIdCallBack = update.getCallbackQuery().getMessage().getChatId();

                    if (callBackData.equals("balance_add")) {

                        person.setBalance(person.getBalance() + money);
                        String text = "Ваш  баланс пополнен! Нажмите /confirmed";
                        person.setState(2);
                        personDAO.update(person.getId(), person);
                        EditMessageText messageText = new EditMessageText();
                        messageText.setChatId(chatIdCallBack);
                        messageText.setText(text);
                        messageText.setMessageId((int) messageId);
                        execute(messageText);
                    }
                    if(callBackData.equals("balance_remove")){
                        person.setBalance(person.getBalance() - money);

                        String text = "Деньги выведены! Нажмите /confirmed";
                        person.setState(2);
                        personDAO.update(person.getId(), person);
                        EditMessageText messageText = new EditMessageText();
                        messageText.setChatId(chatIdCallBack);
                        messageText.setText(text);
                        messageText.setMessageId((int) messageId);
                        execute(messageText);
                    }
                }

            //////////////////////////////////////////////////////////////
            //Тут я работаю над реализовыванием аренды машины клиентом
            //////////////////////////////////////////////////////////////
        }
        else if (person.getState() == 5 && person.getRole().equals("client")) {
            List<Car> cars = carDAO.index();


            if(cars.size() == 0){
                sendMessage.setText("Извините, сейчас нет доступных машин!");
                execute(sendMessage);
            }

            if(update.hasCallbackQuery() && update.getCallbackQuery().getData().length() < 4){


                int id = Integer.parseInt(update.getCallbackQuery().getData());
                Car car = carDAO.show(id);

                InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
                List<InlineKeyboardButton> rowInLine = new ArrayList<>();

                InlineKeyboardButton hour = new InlineKeyboardButton();
                InlineKeyboardButton day = new InlineKeyboardButton();

                hour.setText("Арендовать в час");
                hour.setCallbackData("Hour"+car.getId());
                day.setText("Арендовать в день");
                day.setCallbackData("Day"+car.getId());

                rowInLine.add(hour);
                rowInLine.add(day);
                rowsInLine.add(rowInLine);
                markupInLine.setKeyboard(rowsInLine);


                sendMessage.setText(person.getUsername()+", выбрали машину "+car.getName()
                +"\nЦена на день :"+car.getPricePerDay()+"" +
                        "\nЦена на час :"+car.getPricePerHour());

                sendMessage.setReplyMarkup(markupInLine);
                execute(sendMessage);



            }else if(update.hasCallbackQuery() && update.getCallbackQuery().getData().contains("Hour")){
                int id = Integer.parseInt(update.getCallbackQuery().getData().replace("Hour",""));
                Car car = carDAO.show(id);
                if(car.getPricePerHour() > person.getBalance()){
                    sendMessage.setText("У вас не хватает денег!\n" +
                            "Вы были перенаправлены в меню /menu");
                    execute(sendMessage);
                    person.setState(2);
                    personDAO.update(person.getId(),person);
                }else {



                    LocalDateTime time = LocalDateTime.now();
                    time = time.plusHours(1);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d.M.yyyy HH:mm");
                    String message = time.format(formatter);
                    sendMessage.setText("Если вы подтвердите аренду, то автомобиль будет арендован до " + message
                            + "\nМы можем предоставить аренду только на час! Относитесь к этому серьезно!" +
                            "\nНажмите на /ConfirmHour" + id + "\nЧтобы начать аренду!");
                    execute(sendMessage);

                }
            }

            else if(update.hasCallbackQuery() && update.getCallbackQuery().getData().contains("Day")){
                int id = Integer.parseInt(update.getCallbackQuery().getData().replace("Day",""));
                Car car = carDAO.show(id);
                if(car.getPricePerDay() > person.getBalance()){
                    sendMessage.setText("У вас не хватает денег!\n" +
                            "Вы были перенаправлены в меню /menu");
                    execute(sendMessage);
                    person.setState(2);
                    personDAO.update(person.getId(),person);
                }else {
                    sendMessage.setText("Вы перешли к аренде автомобиля на день!");
                    execute(sendMessage);
                    LocalDateTime time = LocalDateTime.now();
                    time = time.plusHours(24);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d.M.yyyy HH:mm");
                    String message = time.format(formatter);


                    sendMessage.setText("Если вы подтвердите аренду, то автомобиль будет арендован до " + message
                            + "\nМы можем предоставить аренду только на день! Относитесь к этому серьезно!" +
                            "\nНажмите на /ConfirmDay" + id + "\nЧтобы начать аренду!");
                    execute(sendMessage);
                }

            }
            else if(update.getMessage().getText().equals("Машины") && !update.hasCallbackQuery()){

                for(Car car:cars){
                    InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
                    List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
                    List<InlineKeyboardButton> rowInLine = new ArrayList<>();

                    InlineKeyboardButton carsList = new InlineKeyboardButton();

                    carsList.setText("Перейти к аренде "+car.getName());
                    carsList.setCallbackData(String.valueOf(car.getId()));

                    rowInLine.add(carsList);
                    rowsInLine.add(rowInLine);
                    markupInLine.setKeyboard(rowsInLine);
                    SendMessage sendMessage1 = new SendMessage();
                    sendMessage1.setChatId(chatId);
                    sendMessage1.setText("Машина номер :"+car.getId() +
                            "\nНазвание: "+car.getName()+"\nЦена в час:"+car.getPricePerHour()+"" +
                            "\nЦена в день: "+car.getPricePerDay()+"\n");

                    SendPhoto sendPhoto1 = new SendPhoto();
                    sendMessage1.setReplyMarkup(markupInLine);
                    sendPhoto1.setChatId(chatId);
                    sendPhoto1.setPhoto(new InputFile(car.getPicture()));
                    execute(sendMessage1);
                    execute(sendPhoto1);
                }
            }else if(update.getMessage().getText().equals("Назад")){
                person.setState(2);
                sendMessage.setText("Вы перенаправлены в главное меню\nНажмите на след текст: /menu\n" +
                        "Для появления пунктов меню!");
                execute(sendMessage);
                personDAO.update(person.getId(),person);
            }else if(update.getMessage().getText().contains("Confirm")){
                Car car = new Car();
                int id = 0;
                LocalDateTime time = LocalDateTime.now();
                String message= "";
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d.M.yyyy HH:mm");
                Person manager = new Person();
                if(update.getMessage().getText().contains("Day")) {
                    LocalDateTime dayPlus = time.plusDays(1);
                    message = dayPlus.format(formatter);
                    id = Integer.parseInt(update.getMessage().getText().replace("/ConfirmDay",""));
                    car = carDAO.show(id);
                    manager = personDAO.show(car.getManagerId());
                    person.setBalance(person.getBalance()-car.getPricePerDay());
                    manager.setBalance(manager.getBalance()+car.getPricePerDay());
                } else if(update.getMessage().getText().contains("Hour")){
                    LocalDateTime hourPlus = time.plusHours(1);
                    message = hourPlus.format(formatter);
                    id = Integer.parseInt(update.getMessage().getText().replace("/ConfirmHour",""));
                    car = carDAO.show(id);
                    manager = personDAO.show(car.getManagerId());
                    person.setBalance(person.getBalance()-car.getPricePerHour());
                    manager.setBalance(manager.getBalance()+car.getPricePerHour());
                }
                sendMessage.setText("Вы начали аренду автомобиля "+car.getName()+"" +
                        "\nДо "+message);
                execute(sendMessage);
                car.setTime(message);
                carDAO.carRented(id,car);
                person.setState(6);
                personDAO.addCar(id,person.getId());
                personDAO.update(person.getId(),person);
                personDAO.update(manager.getId(), manager);
            }

        }
        else if(person.getState()== 6 && person.getRole().equals("client")){
            person = personDAO.show(person.getChatId());
            int carId = person.getCar();
            Car car = carDAO.show(carId);

            String dateString = car.getTime();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d.M.yyyy HH:mm");
            LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);
            LocalDateTime now = LocalDateTime.now();
            if(dateTime.isBefore(now)) {
                sendMessage.setText("Аренда закончена\nМожете продолжать пользоваться ботом! /menu");
                person.setState(2);
                person.setCar(0);
                carDAO.rentEnded(carId);
                personDAO.update(person.getId(),person);
                personDAO.carRented(person.getId());
            }else{
                sendMessage.setText("Идет аренда автомобиля "+car.getName()+", возможность пользоваться ботом ограничена!" +
                        "\nАренда идет до "+dateString);

            }
            execute(sendMessage);
        }

        //////////////////////////////////////////////////////////////
        //Тут я работаю над профилем менеджера
        //////////////////////////////////////////////////////////////
        else if(person.getRole().equals("manager") && person.getState() == 2){
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

                cars.setText("Добавить машину");
                cars.setCallbackData("cars");

                rowInLine.add(profile);
                rowInLine.add(balance);
                rowInLine.add(cars);

                rowsInLine.add(rowInLine);

                markupInLine.setKeyboard(rowsInLine);
                SendMessage newMessage = new SendMessage();
                newMessage.setChatId(chatId);
                newMessage.setText("Привет менеджер, " + update.getMessage().getChat().getUserName() + "" +
                        "\nТы находишься в главном меню!" +
                        "\nВыбери действие!");
                newMessage.setReplyMarkup(markupInLine);
                execute(newMessage);
            }else if (update.hasCallbackQuery()) {

                String callBackData = update.getCallbackQuery().getData();
                long messageId = update.getCallbackQuery().getMessage().getMessageId();
                long chatIdCallBack = update.getCallbackQuery().getMessage().getChatId();

                if (callBackData.equals("profile")) {
                    ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
                    List<KeyboardRow> rows = new ArrayList<>();
                    KeyboardRow row = new KeyboardRow();
                    row.add("Назад");
                    rows.add(row);
                    markup.setKeyboard(rows);
                    markup.setOneTimeKeyboard(true);
                    markup.setResizeKeyboard(true);
                    person = personDAO.show(chatId);
                    String text = "Ваш никнейм: " + person.getUsername()
                            + "\nВаш баланс: " + person.getBalance() + " рублей";
                    person.setState(3);
                    personDAO.update(person.getId(), person);
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
                    row.add("Вывести деньги");
                    rows.add(row);
                    markup.setResizeKeyboard(true);
                    markup.setKeyboard(rows);
                    markup.setOneTimeKeyboard(true);
                    person = personDAO.show(chatId);
                    String text = "Ваш баланс: " + person.getBalance() + " рублей" +
                            "\nВы можете пополнить баланс нажав на кнопку пополнить баланс" +
                            "\nМинимальная сумма пополнения 100 рублей!";
                    person.setState(4);
                    personDAO.update(person.getId(), person);
                    EditMessageText messageText = new EditMessageText();
                    messageText.setChatId(chatIdCallBack);
                    messageText.setText(text);
                    messageText.setMessageId((int) messageId);
                    sendMessage.setText("Выберите действие");
                    sendMessage.setReplyMarkup(markup);
                    execute(messageText);
                    execute(sendMessage);
                } else if (callBackData.equals("cars")) {

                    person.setState(5);
                    personDAO.update(person.getId(), person);
                    ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
                    List<KeyboardRow> rows = new ArrayList<>();
                    KeyboardRow row = new KeyboardRow();
                    row.add("Назад");
                    row.add("Перейти к добавлению");
                    rows.add(row);
                    markup.setKeyboard(rows);
                    markup.setOneTimeKeyboard(true);
                    markup.setResizeKeyboard(true);
                    EditMessageText messageText = new EditMessageText();
                    messageText.setChatId(chatIdCallBack);
                    messageText.setText("Вы находитесь в меню машин, введите комманду нажмите на кнопку на экране" +
                            "для добавления машин");
                    messageText.setMessageId((int) messageId);
                    sendMessage.setReplyMarkup(markup);
                    execute(messageText);
                    sendMessage.setText("Выберите действие:");
                    execute(sendMessage);
                }
            }
        }


        //////////////////////////////////////////////////////////////
        //Тут я работаю над добавлением машин
        //////////////////////////////////////////////////////////////
        else if (person.getState() == 5 && person.getRole().equals("manager")){
            if(update.getMessage().getText().equals("Назад")){
                person.setState(2);
                personDAO.update(person.getId(),person);
                sendMessage.setText("Вы были перенапрвлены в главное меню!" +
                        "\n/menu");
                execute(sendMessage);
            } else if (update.getMessage().getText().equals("Перейти к добавлению")) {
                sendMessage.setText(person.getUsername()+", введите название машины!");
                state = 2;
                execute(sendMessage);
            }else
            if(state ==2){
                registrycar.setName(update.getMessage().getText());
                sendMessage.setText("Введите цену в час");
                state = 3;
                execute(sendMessage);
            }else
            if(state ==3){
                registrycar.setPricePerHour(Double.parseDouble(update.getMessage().getText()));
                sendMessage.setText("Введите цену в день!");
                state =4;
                execute(sendMessage);
            }else if(state == 4){
                registrycar.setPricePerDay(Double.parseDouble(update.getMessage().getText()));
                sendMessage.setText("Добавьте ссылку на машину!");
                state =5;
                execute(sendMessage);
            } else if (state ==5) {
                registrycar.setPicture(update.getMessage().getText());
                registrycar.setAvaliable(true);
                registrycar.setTime("0");
                registrycar.setManagerId(chatId);
                sendMessage.setText("Машина была добавлена!" +
                        "\nВы перенапрвлены в главное меню! /menu");
                carDAO.save(registrycar);
                state =0;
                person.setState(2);
                personDAO.update(person.getId(),person);
                execute(sendMessage);
            }
        }
        //ADMIN!!!
        if(chatId == admin){
            if(update.getMessage() != null) {
                if (update.getMessage().getText().contains("/Confirm_")) {
                    String adminMessage = update.getMessage().getText();
                    String[] arr = adminMessage.split("_");
                    long userId = Long.parseLong(arr[1]);
                    sendMessage.setText("Вас подтвердил администратор!" +
                            "\n /menu");
                    sendMessage.setChatId(userId);
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
                    sendMessage.setChatId(userId);
                    execute(sendMessage);
                }
            }
        }
    }





}
