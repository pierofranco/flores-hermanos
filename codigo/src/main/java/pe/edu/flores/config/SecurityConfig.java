package pe.edu.flores.config;
import java.time.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import pe.edu.flores.dao.SqlDao;
@Configuration @EnableMethodSecurity
public class SecurityConfig {
 @Bean PasswordEncoder encoder(){return new BCryptPasswordEncoder(10);}
 @Bean UserDetailsService users(SqlDao db,Clock clock){return name->{var u=db.find("SELECT * FROM usuario WHERE usuario=?",name).orElseThrow(()->new UsernameNotFoundException("Credenciales inválidas"));boolean locked=u.get("bloqueado_hasta")!=null&&((java.sql.Timestamp)u.get("bloqueado_hasta")).toLocalDateTime().isAfter(LocalDateTime.now(clock));return User.withUsername(name).password(SqlDao.text(u,"clave_hash")).roles(SqlDao.text(u,"rol")).disabled(!Boolean.TRUE.equals(u.get("activo"))).accountLocked(locked).build();};}
 @Bean SecurityFilterChain chain(HttpSecurity http,SqlDao db,Clock clock)throws Exception{
  http.addFilterAfter(new AccountStatusFilter(db),org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class);
  http.authorizeHttpRequests(a->a.requestMatchers("/","/index.html","/app.js","/style.css","/qr-reader.js","/assets/**","/favicon.ico","/api/csrf","/api/login","/error").permitAll().anyRequest().authenticated())
   .formLogin(f->f.loginProcessingUrl("/api/login").successHandler((req,res,auth)->{db.update("UPDATE usuario SET intentos=0,bloqueado_hasta=NULL WHERE usuario=?",auth.getName());db.audit(auth.getName(),"LOGIN","Acceso correcto");res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"ok\":true}");}).failureHandler((req,res,e)->{String user=req.getParameter("username");db.update("UPDATE usuario SET intentos=intentos+1 WHERE usuario=?",user);db.find("SELECT intentos FROM usuario WHERE usuario=?",user).ifPresent(u->{if(((Number)u.get("intentos")).intValue()>=5)db.update("UPDATE usuario SET intentos=0,bloqueado_hasta=? WHERE usuario=?",LocalDateTime.now(clock).plusSeconds(60),user);});res.setStatus(401);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\"Credenciales incorrectas, cuenta desactivada o bloqueo temporal. Tras cinco intentos espere 60 segundos.\"}");}).permitAll())
   .logout(l->l.logoutUrl("/api/logout").logoutSuccessHandler((req,res,auth)->{res.setContentType("application/json");res.getWriter().write("{\"ok\":true}");}))
   .exceptionHandling(e->e.authenticationEntryPoint((req,res,x)->{res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"message\":\"Inicie sesión para continuar\"}");}).accessDeniedHandler((req,res,x)->{res.setStatus(403);res.setContentType("application/json");res.getWriter().write("{\"message\":\"Sin permiso o sesión de seguridad vencida. Actualice la página.\"}");}));
  return http.build();
 }
}
