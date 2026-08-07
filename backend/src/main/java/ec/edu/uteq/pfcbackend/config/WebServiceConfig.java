package ec.edu.uteq.pfcbackend.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.soap.server.endpoint.SoapFaultAnnotationExceptionResolver;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

@EnableWs
@Configuration
public class WebServiceConfig {

    private static final String NAMESPACE_URI = "http://uteq.edu.ec/pfc/libro-catalogo";

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(WebApplicationContext contexto) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(contexto);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    // El nombre del bean determina la URL del WSDL: /ws/{nombre}.wsdl
    @Bean(name = "libro-catalogo")
    public DefaultWsdl11Definition libroCatalogoWsdl11Definition(XsdSchema libroCatalogoSchema) {
        DefaultWsdl11Definition definicion = new DefaultWsdl11Definition();
        definicion.setPortTypeName("LibroCatalogoPort");
        definicion.setLocationUri("/ws");
        definicion.setTargetNamespace(NAMESPACE_URI);
        definicion.setSchema(libroCatalogoSchema);
        return definicion;
    }

    @Bean
    public XsdSchema libroCatalogoSchema() {
        return new SimpleXsdSchema(new ClassPathResource("libro-catalogo.xsd"));
    }

    // Marshaller/Unmarshaller para las clases JAXB generadas a partir del XSD
    // (ver plugin jaxb2-maven-plugin en pom.xml, paquete ec.edu.uteq.pfcbackend.ws.gen).
    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("ec.edu.uteq.pfcbackend.ws.gen");
        return marshaller;
    }

    // Traduce excepciones anotadas con @SoapFault (ver LibroNoEncontradoSoapException)
    // a un SOAP Fault real en vez de un 500 generico.
    @Bean
    public SoapFaultAnnotationExceptionResolver exceptionResolver() {
        SoapFaultAnnotationExceptionResolver resolver = new SoapFaultAnnotationExceptionResolver();
        resolver.setOrder(1);
        return resolver;
    }
}
