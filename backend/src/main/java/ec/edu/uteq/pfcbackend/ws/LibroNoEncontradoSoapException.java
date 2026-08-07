package ec.edu.uteq.pfcbackend.ws;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

// Mecanismo idiomatico de Spring-WS para convertir una excepcion en un SOAP Fault real
// (no un 500 generico): @SoapFault + SoapFaultAnnotationExceptionResolver (ver WebServiceConfig).
// Sin faultStringOrReason explicito, Spring-WS usa el mensaje de la excepcion como faultstring.
@SoapFault(faultCode = FaultCode.CLIENT)
public class LibroNoEncontradoSoapException extends RuntimeException {

    public LibroNoEncontradoSoapException(String mensaje) {
        super(mensaje);
    }
}
