package br.com.teamss.skillswap.skill_swap.model.config;

// import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// import org.springframework.web.servlet.LocaleResolver;
// import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
// import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
// import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
// import org.springframework.web.servlet.i18n.SessionLocaleResolver;

// import java.util.Locale;

    @Configuration
    public class MvcConfig{ // implements WebMvcConfigurer {

    // Define como o idioma escolhido será armazenado (neste caso, na sessão do usuário).
    // @Bean
    // public LocaleResolver localeResolver() {
    //    SessionLocaleResolver slr = new SessionLocaleResolver();
    //    slr.setDefaultLocale(new Locale("pt", "BR")); // Idioma padrão caso nenhum seja detectado.
    //    return slr;
    //}

    // Cria um interceptador que mudará o idioma sempre que encontrar um parâmetro na URL.
    // @Bean
    // public LocaleChangeInterceptor localeChangeInterceptor() {
    //    LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
    //    lci.setParamName("lang"); // O idioma será trocado quando a URL tiver "?lang=en", "?lang=pt_BR", etc.
    //    return lci;
    //}

    // Registra o interceptador no Spring.
    // @Override
    // public void addInterceptors(InterceptorRegistry registry) {
    //    registry.addInterceptor(localeChangeInterceptor());
    // }
 }