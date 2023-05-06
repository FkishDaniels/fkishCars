package bot.database;

import bot.models.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
public class PersonDAO {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public PersonDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Person> index(){
        return jdbcTemplate.query("SELECT * FROM PERSON", new BeanPropertyRowMapper<>(Person.class));
    }

    public Person show(long ChatId){
        return jdbcTemplate.query("SELECT * FROM PERSON WHERE ChatId = ?", new Object[]{ChatId},
                new BeanPropertyRowMapper<>(Person.class)).stream().findAny().orElse(null);
    }

    public void save(Person person){
        jdbcTemplate.update("INSERT INTO person(username,password,email,balance,confirmed,chatId,state) VALUES(?,?,?,?,?,?,1)", person.getUsername(), person.getPassword(), person.getEmail(), person.getBalance(),person.isConfirmed(),person.getChatId());
    }

    public void update(int id, Person updatedPerson) {
        jdbcTemplate.update("UPDATE person SET confirmed=?,balance = ?,state=? WHERE id =?", updatedPerson.isConfirmed(), updatedPerson.getBalance(),updatedPerson.getState(),id);
    }

    public void delete(int id) {
        jdbcTemplate.update("DELETE FROM person WHERE id =?", id);
    }

}
