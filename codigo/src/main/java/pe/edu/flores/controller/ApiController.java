package pe.edu.flores.controller;
import java.util.*;
import java.time.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import javax.imageio.ImageIO;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.csrf.CsrfToken;
import jakarta.validation.Valid;
import pe.edu.flores.dao.*;
import pe.edu.flores.service.*;
import pe.edu.flores.model.Commands.*;
import com.google.zxing.*;
import com.google.zxing.client.j2se.*;
import com.google.zxing.common.*;
import com.google.zxing.qrcode.QRCodeWriter;
@RestController @RequestMapping("/api")
public class ApiController {
 private final SqlDao db;private final IdentityService me;private final CatalogoService catalog;private final VentaService sales;private final EmbarqueService boarding;private final ManifiestoService manifest;private final FidelizacionService points;private final ReportesService reports;private final Clock clock;private final BackupService backupService;
 public ApiController(SqlDao db,IdentityService me,CatalogoService catalog,VentaService sales,EmbarqueService boarding,ManifiestoService manifest,FidelizacionService points,ReportesService reports,Clock clock,BackupService backupService){this.db=db;this.me=me;this.catalog=catalog;this.sales=sales;this.boarding=boarding;this.manifest=manifest;this.points=points;this.reports=reports;this.clock=clock;this.backupService=backupService;}
 @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken(),"header",token.getHeaderName());}
 @GetMapping("/me") public Map<String,Object> me(){return Map.of("usuario",me.current(),"modo","DEMOSTRACION","zonaHoraria","America/Lima","fecha",LocalDate.now(clock),"politica","Devolución simulada del 100% antes de salida. Puntos: 1 por cada 10 km, valor de canje S/ 0.10. Políticas académicas configurables.");}
 @GetMapping("/mi-pasajero") @PreAuthorize("hasRole('PASAJERO')") public Object ownPassenger(){return db.one("SELECT * FROM pasajero WHERE id=?",me.current().get("pasajero_id"));}
 @GetMapping("/viajes") public Object trips(){return reports.trips();}
 @GetMapping("/viajes/{id}/asientos") public Object seats(@PathVariable long id){return sales.seats(id);}
 @PostMapping("/reservas") @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR','PASAJERO')") public Object reserve(@Valid @RequestBody Reserva r){return sales.reserve(r);}
 @DeleteMapping("/reservas/{token}") @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR','PASAJERO')") public void release(@PathVariable String token){sales.release(token);}
 @GetMapping("/cotizar") @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR','PASAJERO')") public Object quote(@RequestParam long viajeId,@RequestParam long pasajeroId,@RequestParam(defaultValue="")String promocion,@RequestParam(defaultValue="0")int puntos){return sales.quote(viajeId,pasajeroId,promocion,puntos);}
 @PostMapping("/ventas") @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR','PASAJERO')") public Object sell(@Valid @RequestBody Venta cmd){return sales.sell(cmd);}
 @GetMapping("/boletos") public Object tickets(){return sales.tickets();}
 @GetMapping("/boletos/{id}") public Object ticket(@PathVariable long id){return sales.ticket(id);}
 @PostMapping("/boletos/{id}/anular") @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR','PASAJERO')") public Object cancel(@PathVariable long id){return sales.cancel(id);}
 @PostMapping("/boletos/{id}/reprogramar") @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR','PASAJERO')") public Object reschedule(@PathVariable long id,@Valid @RequestBody Reprogramacion cmd){return sales.reprogram(id,cmd);}
 @GetMapping(value="/boletos/{id}/qr",produces=MediaType.IMAGE_PNG_VALUE) public byte[] qr(@PathVariable long id)throws Exception{var ticket=sales.ticket(id);var matrix=new QRCodeWriter().encode(ticket.get("qr").toString(),BarcodeFormat.QR_CODE,320,320,Map.of(EncodeHintType.MARGIN,2));ByteArrayOutputStream o=new ByteArrayOutputStream();MatrixToImageWriter.writeToStream(matrix,"PNG",o);return o.toByteArray();}
 @PostMapping("/viajes/{id}/embarcar") @PreAuthorize("hasAnyRole('ADMIN','CONTROLADOR')") public Object board(@PathVariable long id,@Valid @RequestBody Embarque cmd){return boarding.board(id,cmd);}
 @PostMapping("/qr/leer") @PreAuthorize("hasAnyRole('ADMIN','CONTROLADOR')") public Object decode(@RequestParam("imagen")MultipartFile image)throws Exception{
  if(image.isEmpty()||image.getSize()>5*1024*1024)throw new BusinessException("Seleccione una imagen QR de hasta 5 MB");
  try(var input=ImageIO.createImageInputStream(image.getInputStream())){var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new BusinessException("Archivo de imagen no reconocido");var reader=readers.next();try{reader.setInput(input);if((long)reader.getWidth(0)*reader.getHeight(0)>20000000)throw new BusinessException("La imagen excede veinte megapíxeles");var bitmap=new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(reader.read(0))));return Map.of("texto",new MultiFormatReader().decode(bitmap,Map.of(DecodeHintType.TRY_HARDER,true)).getText());}catch(NotFoundException e){throw new BusinessException("No se detectó un código QR legible");}finally{reader.dispose();}}
 }
 @GetMapping("/viajes/{id}/manifiesto") @PreAuthorize("hasAnyRole('ADMIN','CONTROLADOR','JEFE_OPERACIONES')") public Object manifest(@PathVariable long id){return manifest.detail(id);}
 @PostMapping("/viajes/{id}/manifiesto") @PreAuthorize("hasAnyRole('ADMIN','CONTROLADOR','JEFE_OPERACIONES')") public Object generate(@PathVariable long id){return manifest.generate(id);}
 @PostMapping("/viajes/{id}/manifiesto/enviar") @PreAuthorize("hasAnyRole('ADMIN','CONTROLADOR','JEFE_OPERACIONES')") public Object send(@PathVariable long id,@RequestBody Map<String,Object> b){return manifest.send(id,Boolean.TRUE.equals(b.get("simularError")));}
 @GetMapping("/viajes/{id}/manifiesto.csv") @PreAuthorize("hasAnyRole('ADMIN','CONTROLADOR','JEFE_OPERACIONES')") public ResponseEntity<byte[]> manifestCsv(@PathVariable long id){@SuppressWarnings("unchecked")var rows=(List<Map<String,Object>>)manifest.detail(id).get("pasajeros");return file(reports.csv(rows).getBytes(StandardCharsets.UTF_8),"manifiesto-"+id+".csv","text/csv;charset=UTF-8");}
 @GetMapping("/fidelizacion/{id}") public Object points(@PathVariable long id){return points.history(id);}
 @GetMapping("/catalogos") @PreAuthorize("!hasRole('PASAJERO')") public Object catalogs(){return catalog.metadata();}
 @GetMapping("/catalogos/{key}") @PreAuthorize("!hasRole('PASAJERO')") public Object catalog(@PathVariable String key){return catalog.list(key);}
 @PostMapping("/catalogos/{key}") @PreAuthorize("hasAnyRole('ADMIN','JEFE_OPERACIONES','VENDEDOR')") public Object create(@PathVariable String key,@RequestBody Map<String,Object> data){writeAllowed(key);return Map.of("id",catalog.save(key,null,data));}
 @PutMapping("/catalogos/{key}/{id}") @PreAuthorize("hasAnyRole('ADMIN','JEFE_OPERACIONES','VENDEDOR')") public Object update(@PathVariable String key,@PathVariable long id,@RequestBody Map<String,Object> data){writeAllowed(key);return Map.of("id",catalog.save(key,id,data));}
 private void writeAllowed(String key){if(me.role().equals("VENDEDOR")&&!key.equals("pasajeros"))throw new org.springframework.security.access.AccessDeniedException("Operación reservada a jefatura");}
 @GetMapping("/usuarios") @PreAuthorize("hasRole('ADMIN')") public Object users(){return db.list("SELECT id,usuario,rol,empleado_id,pasajero_id,activo,intentos,bloqueado_hasta FROM usuario ORDER BY id");}
 @PostMapping("/usuarios") @PreAuthorize("hasRole('ADMIN')") public void user(@RequestBody Map<String,Object> data){catalog.user(data);}
 @PutMapping("/usuarios/{id}/estado") @PreAuthorize("hasRole('ADMIN')") public void status(@PathVariable long id,@RequestBody Map<String,Object> data){catalog.toggleUser(id,Boolean.TRUE.equals(data.get("activo")));}
 @PutMapping("/usuarios/{id}/clave") @PreAuthorize("hasRole('ADMIN')") public void password(@PathVariable long id,@RequestBody Map<String,String> data){catalog.changePassword(id,data.get("clave"));}
 @GetMapping("/reportes") @PreAuthorize("hasAnyRole('ADMIN','JEFE_OPERACIONES')") public Object report(@RequestParam LocalDate desde,@RequestParam LocalDate hasta){return reports.report(desde,hasta);}
 @GetMapping("/reportes.csv") @PreAuthorize("hasAnyRole('ADMIN','JEFE_OPERACIONES')") public ResponseEntity<byte[]> csv(@RequestParam LocalDate desde,@RequestParam LocalDate hasta){@SuppressWarnings("unchecked")var rows=(List<Map<String,Object>>)reports.report(desde,hasta).get("ocupacion");return file(reports.csv(rows).getBytes(StandardCharsets.UTF_8),"ocupacion.csv","text/csv;charset=UTF-8");}
 @GetMapping("/auditoria") @PreAuthorize("hasRole('ADMIN')") public Object audit(){return db.list("SELECT * FROM auditoria ORDER BY id DESC LIMIT 200");}
 @PostMapping("/backup") @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<byte[]> backup()throws Exception{return file(backupService.backup(),"flores-postgresql.backup","application/octet-stream");}
 @GetMapping("/rsu") @PreAuthorize("hasAnyRole('ADMIN','JEFE_OPERACIONES')") public Object rsu(){return db.list("SELECT id,tipo,titulo,descripcion,referencia,fecha,registrado_por FROM evidencia_rsu ORDER BY id DESC");}
 @PostMapping("/rsu") @PreAuthorize("hasAnyRole('ADMIN','JEFE_OPERACIONES')") public Object rsu(@RequestBody Map<String,Object> d){String type=Objects.toString(d.get("tipo"),"");if(!Set.of("ENTREGA","RECOMENDACION","VALIDACION").contains(type))throw new BusinessException("Tipo de evidencia inválido");String title=Objects.toString(d.get("titulo"),"").trim(),description=Objects.toString(d.get("descripcion"),"").trim(),ref=Objects.toString(d.get("referencia"),"").trim();if(title.isBlank()||title.length()>150||description.isBlank()||description.length()>2000||ref.length()>500)throw new BusinessException("Complete título y descripción respetando su extensión");LocalDate date=LocalDate.parse(Objects.toString(d.get("fecha"),""));if(date.isAfter(LocalDate.now(clock)))throw new BusinessException("La evidencia no puede tener una fecha futura");long id=db.insert("INSERT INTO evidencia_rsu(tipo,titulo,descripcion,referencia,fecha,registrado_por) VALUES(?,?,?,?,?,?)",type,title,description,ref,date,me.username());db.audit(me.username(),"REGISTRO_RSU","Evidencia #"+id+" declarada por el equipo; conservar archivo original");return Map.of("id",id);}
 private ResponseEntity<byte[]> file(byte[]b,String name,String type){return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(name).build().toString()).contentType(MediaType.parseMediaType(type)).body(b);}
}
