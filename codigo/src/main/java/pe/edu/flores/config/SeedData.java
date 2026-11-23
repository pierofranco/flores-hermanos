package pe.edu.flores.config;
import java.time.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.flores.dao.SqlDao;
@Component
public class SeedData implements CommandLineRunner {
 private final SqlDao db;private final PasswordEncoder enc;private final Clock clock;
 @Value("${flores.seed:true}")boolean enabled;
 @Value("${flores.demo-password}")String password;
 public SeedData(SqlDao db,PasswordEncoder enc,Clock clock){this.db=db;this.enc=enc;this.clock=clock;}
 @Override @Transactional public void run(String...args){
  if(!enabled||db.count("SELECT COUNT(*) FROM usuario")>0)return;
  var today=LocalDate.now(clock);
  long agency=db.insert("INSERT INTO agencia(nombre,ciudad,direccion,telefono) VALUES(?,?,?,?)","Agencia de demostración Lima","Lima","Dirección de prueba","000000000");
  String[][] staff={{"admin","ADMIN","Administración"},{"vendedor","VENDEDOR","Ventas"},{"controlador","CONTROLADOR","Embarque"},{"jefe","JEFE_OPERACIONES","Operaciones"}};
  for(int i=0;i<staff.length;i++){long employee=db.insert("INSERT INTO empleado(agencia_id,nombres,apellidos,documento,cargo) VALUES(?,?,?,?,?)",agency,staff[i][2],"Demo","9000000"+i,staff[i][1]);db.update("INSERT INTO usuario(usuario,clave_hash,rol,empleado_id) VALUES(?,?,?,?)",staff[i][0],enc.encode(password),staff[i][1],employee);}
  long passenger=db.insert("INSERT INTO pasajero(tipo_documento,documento,nombres,apellidos,correo) VALUES('DNI',?,?,?,?)","99999991","Pasajero","Demostración","prueba@example.invalid");
  long account=db.insert("INSERT INTO cuenta_fidelizacion(pasajero_id,saldo) VALUES(?,?)",passenger,300);
  db.update("INSERT INTO movimiento_puntos(cuenta_id,tipo,puntos) VALUES(?,'DEVOLUCION',300)",account);
  db.update("INSERT INTO usuario(usuario,clave_hash,rol,pasajero_id) VALUES(?,?,?,?)","viajero",enc.encode(password),"PASAJERO",passenger);
  String[] cities={"Ica","Arequipa","Tacna","Moquegua","Nazca","Cusco"};int[] km={303,1010,1290,1150,450,1100};int[] mins={270,960,1200,1080,420,1260};int[] fare={65,120,150,140,85,160};
  for(int i=0;i<cities.length;i++){
   long route=db.insert("INSERT INTO ruta(origen,destino,distancia_km,duracion_min) VALUES(?,?,?,?)","Lima",cities[i],km[i],mins[i]);
   long bus=db.insert("INSERT INTO bus(placa,marca,modelo,capacidad,soat_hasta,revision_hasta,estado) VALUES(?,?,?,?,?,?,'OPERATIVO')","DEM-10"+i,"Bus de prueba","Cama 40",40,today.plusYears(1),today.plusYears(1));
   for(int n=1;n<=40;n++)db.update("INSERT INTO asiento(bus_id,numero,piso,tipo) VALUES(?,?,?,?)",bus,n,n<=12?1:2,n<=12?"CAMA":"SEMICAMA");
   long driver=db.insert("INSERT INTO conductor(nombres,apellidos,licencia,categoria,vencimiento) VALUES(?,?,?,?,?)","Conductor "+(i+1),"Demostración","DEMO-LIC-"+i,"AIIIC",today.plusYears(2));
   for(int d=1;d<=3;d++){long trip=db.insert("INSERT INTO viaje(ruta_id,bus_id,conductor_id,salida,precio,estado) VALUES(?,?,?,?,?,'PROGRAMADO')",route,bus,driver,today.plusDays(d*2L+i%2).atTime(8+i*2,0),fare[i]);db.update("INSERT INTO reserva(viaje_id,asiento_id) SELECT ?,id FROM asiento WHERE bus_id=?",trip,bus);}
  }
  db.update("INSERT INTO promocion(nombre,codigo,tipo,valor,desde,hasta) VALUES(?,?,'PORCENTAJE',10,?,?)","Bienvenida digital de demostración","FLORES10",today.minusDays(1),today.plusYears(1));
  db.audit("sistema","INICIALIZACION","Datos ficticios de demostración. Saldo inicial de ejemplo: 300 puntos.");
 }
}
