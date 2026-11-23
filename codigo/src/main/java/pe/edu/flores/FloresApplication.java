package pe.edu.flores;
import java.time.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
@SpringBootApplication
public class FloresApplication {
 public static void main(String[] args){SpringApplication.run(FloresApplication.class,args);}
 @Bean public Clock clock(){return Clock.system(ZoneId.of("America/Lima"));}
}
