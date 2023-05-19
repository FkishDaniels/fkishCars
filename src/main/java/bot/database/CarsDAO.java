package bot.database;

import bot.models.Car;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
public class CarsDAO {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public CarsDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Car> index(){
        return jdbcTemplate.query("SELECT * FROM Car where avaliable = true", new BeanPropertyRowMapper<>(Car.class));
    }

    public Car show(long carId){
        return jdbcTemplate.query("SELECT * FROM Car WHERE id = ?", new Object[]{carId},
                new BeanPropertyRowMapper<>(Car.class)).stream().findAny().orElse(null);
    }

    public void save(Car car){
        jdbcTemplate.update("INSERT INTO car(name,priceperhour,priceperday,picture,avaliable,time,managerid) VALUES(?,?,?,?,?,?,?)", car.getName(),car.getPricePerHour(),car.getPricePerDay(),car.getPicture(),car.isAvaliable(),car.getTime(),car.getManagerId());
    }

    public void update(int id, Car updatedCar) {
        jdbcTemplate.update("UPDATE car SET pricePerhHour=?,pricePerDay = ?,picture=? WHERE id =?", updatedCar.getPricePerHour(),updatedCar.getPricePerDay(),updatedCar.getPicture(),id);
    }
    public void carRented(int id,Car car){
        jdbcTemplate.update("UPDATE car set avaliable = false,time = ? where id =?", car.getTime(),id);
    }
    public void rentEnded(int id){
        jdbcTemplate.update("UPDATE car set avaliable = true, time = 0 where id =?", id);
    }

    public void delete(int id) {
        jdbcTemplate.update("DELETE FROM car WHERE id =?", id);
    }

}
