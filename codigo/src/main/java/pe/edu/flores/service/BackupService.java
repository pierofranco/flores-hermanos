package pe.edu.flores.service;
import java.io.*;
import java.nio.file.*;
import java.net.URI;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.edu.flores.dao.SqlDao;
@Service
public class BackupService {
 @Value("${spring.datasource.url}")String url;
 @Value("${spring.datasource.username}")String user;
 @Value("${spring.datasource.password:}")String password;
 @Value("${FLORES_PG_BIN:}")String bin;
 private final SqlDao db;private final IdentityService identity;
 public BackupService(SqlDao db,IdentityService identity){this.db=db;this.identity=identity;}
 public byte[] backup()throws Exception{
  URI uri=URI.create(url.substring(5));String exe=bin.isBlank()?"pg_dump":Path.of(bin,"pg_dump"+(System.getProperty("os.name").toLowerCase().contains("win")?".exe":"")).toString();
  if(bin.isBlank()&&System.getProperty("os.name").toLowerCase().contains("win")){for(String v:List.of("18","17","16","15")){Path p=Path.of("C:\\Program Files\\PostgreSQL",v,"bin","pg_dump.exe");if(Files.isRegularFile(p)){exe=p.toString();break;}}}
  Path dump=Files.createTempFile("flores-pg-",".backup"),errors=Files.createTempFile("flores-pg-",".log");
  try{
   ProcessBuilder builder=new ProcessBuilder(exe,"--host",uri.getHost(),"--port",Integer.toString(uri.getPort()<0?5432:uri.getPort()),"--username",user,"--dbname",uri.getPath().substring(1),"--format=custom","--no-owner","--file",dump.toString());builder.environment().put("PGPASSWORD",password);builder.environment().put("PGCONNECT_TIMEOUT","8");builder.redirectError(errors.toFile());builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);Process process;
   try{process=builder.start();}catch(IOException e){throw new BusinessException("Instale las herramientas de PostgreSQL y configure FLORES_PG_BIN con su carpeta bin");}
   if(!process.waitFor(45,TimeUnit.SECONDS)){process.destroyForcibly();throw new BusinessException("El respaldo excedió el tiempo de espera");}if(process.exitValue()!=0)throw new BusinessException("pg_dump no pudo completar el respaldo. Verifique versión, conexión y permisos del usuario de PostgreSQL.");if(Files.size(dump)>100*1024*1024)throw new BusinessException("Respaldo mayor de 100 MB. Use pg_dump desde la terminal siguiendo el manual.");db.audit(identity.username(),"RESPALDO_POSTGRESQL","Copia en formato personalizado generada mediante pg_dump");return Files.readAllBytes(dump);
  }finally{Files.deleteIfExists(dump);Files.deleteIfExists(errors);}
 }
}
