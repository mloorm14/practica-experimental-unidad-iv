# Ejemplo SOAP vs REST — Servicio de consulta de libro por ISBN

Este documento respalda con evidencia real (no inventada) el resultado de aprendizaje del PDF
que exige "exponiendo e integrando servicios web REST y SOAP". Todo el contenido de este archivo
(WSDL, requests, responses, tamaños de payload) se obtuvo probando en vivo la aplicación levantada
localmente, no fue redactado a mano ni copiado de documentación genérica.

## 1. El servicio SOAP

- Contrato XSD: `backend/src/main/resources/libro-catalogo.xsd` (contract-first).
- Endpoint Java: `ec.edu.uteq.pfcbackend.ws.LibroCatalogoEndpoint`.
- Namespace: `http://uteq.edu.ec/pfc/libro-catalogo`.
- URL del servicio: `POST http://localhost:8080/ws`.
- WSDL generado automáticamente por Spring-WS a partir del XSD: `GET http://localhost:8080/ws/libro-catalogo.wsdl`.

### 1.1. WSDL real generado (obtenido en vivo, `GET /ws/libro-catalogo.wsdl`, HTTP 200)

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?><wsdl:definitions xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/" xmlns:sch="http://uteq.edu.ec/pfc/libro-catalogo" xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/" xmlns:tns="http://uteq.edu.ec/pfc/libro-catalogo" targetNamespace="http://uteq.edu.ec/pfc/libro-catalogo">
  <wsdl:types>
    <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" elementFormDefault="qualified" targetNamespace="http://uteq.edu.ec/pfc/libro-catalogo">

    <xs:element name="ConsultarLibroPorIsbnRequest">
        <xs:complexType>
            <xs:sequence>
                <xs:element name="isbn" type="xs:string"/>
            </xs:sequence>
        </xs:complexType>
    </xs:element>

    <xs:element name="ConsultarLibroPorIsbnResponse">
        <xs:complexType>
            <xs:sequence>
                <xs:element name="id" type="xs:long"/>
                <xs:element name="titulo" type="xs:string"/>
                <xs:element minOccurs="0" name="autor" type="xs:string"/>
                <xs:element name="isbn" type="xs:string"/>
                <xs:element name="editorial" type="xs:string"/>
                <xs:element name="idioma" type="xs:string"/>
                <xs:element name="estado" type="xs:string"/>
                <xs:element name="stock" type="xs:int"/>
            </xs:sequence>
        </xs:complexType>
    </xs:element>

    <!-- Contrato del detalle de fault cuando el ISBN no existe. La implementacion actual del
         endpoint usa el mecanismo idiomatico de Spring-WS (@SoapFault + SoapFaultAnnotationExceptionResolver),
         que puebla faultcode/faultstring de forma estandar; este elemento documenta el contrato de
         datos del error para el consumidor del servicio. -->
    <xs:element name="LibroNoEncontradoFault">
        <xs:complexType>
            <xs:sequence>
                <xs:element name="mensaje" type="xs:string"/>
            </xs:sequence>
        </xs:complexType>
    </xs:element>

</xs:schema>
  </wsdl:types>
  <wsdl:message name="ConsultarLibroPorIsbnResponse">
    <wsdl:part element="tns:ConsultarLibroPorIsbnResponse" name="ConsultarLibroPorIsbnResponse">
    </wsdl:part>
  </wsdl:message>
  <wsdl:message name="ConsultarLibroPorIsbnRequest">
    <wsdl:part element="tns:ConsultarLibroPorIsbnRequest" name="ConsultarLibroPorIsbnRequest">
    </wsdl:part>
  </wsdl:message>
  <wsdl:message name="LibroNoEncontradoFault">
    <wsdl:part element="tns:LibroNoEncontradoFault" name="LibroNoEncontradoFault">
    </wsdl:part>
  </wsdl:message>
  <wsdl:portType name="LibroCatalogoPort">
    <wsdl:operation name="ConsultarLibroPorIsbn">
      <wsdl:input message="tns:ConsultarLibroPorIsbnRequest" name="ConsultarLibroPorIsbnRequest">
    </wsdl:input>
      <wsdl:output message="tns:ConsultarLibroPorIsbnResponse" name="ConsultarLibroPorIsbnResponse">
    </wsdl:output>
    </wsdl:operation>
    <wsdl:operation name="LibroNoEncontrado">
      <wsdl:fault message="tns:LibroNoEncontradoFault" name="LibroNoEncontradoFault">
    </wsdl:fault>
    </wsdl:operation>
  </wsdl:portType>
  <wsdl:binding name="LibroCatalogoPortSoap11" type="tns:LibroCatalogoPort">
    <soap:binding style="document" transport="http://schemas.xmlsoap.org/soap/http"/>
    <wsdl:operation name="ConsultarLibroPorIsbn">
      <soap:operation soapAction=""/>
      <wsdl:input name="ConsultarLibroPorIsbnRequest">
        <soap:body use="literal"/>
      </wsdl:input>
      <wsdl:output name="ConsultarLibroPorIsbnResponse">
        <soap:body use="literal"/>
      </wsdl:output>
    </wsdl:operation>
    <wsdl:operation name="LibroNoEncontrado">
      <soap:operation soapAction=""/>
      <wsdl:fault name="LibroNoEncontradoFault">
        <soap:fault name="LibroNoEncontradoFault" use="literal"/>
      </wsdl:fault>
    </wsdl:operation>
  </wsdl:binding>
  <wsdl:service name="LibroCatalogoPortService">
    <wsdl:port binding="tns:LibroCatalogoPortSoap11" name="LibroCatalogoPortSoap11">
      <soap:address location="http://localhost:8080/ws"/>
    </wsdl:port>
  </wsdl:service>
</wsdl:definitions>
```

Nótese que Spring-WS infirió automáticamente la operación de fault `LibroNoEncontrado` a partir del
elemento `LibroNoEncontradoFault` del XSD (convención `{Nombre}Fault` → operación `{Nombre}`), sin
configuración adicional.

### 1.2. Request SOAP real (caso: ISBN existente)

Enviado con `curl -X POST http://localhost:8080/ws -H "Content-Type: text/xml; charset=utf-8" --data-binary @request.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:lib="http://uteq.edu.ec/pfc/libro-catalogo">
  <soapenv:Header/>
  <soapenv:Body>
    <lib:ConsultarLibroPorIsbnRequest>
      <lib:isbn>978-0-451-52493-5</lib:isbn>
    </lib:ConsultarLibroPorIsbnRequest>
  </soapenv:Body>
</soapenv:Envelope>
```

### 1.3. Response SOAP real (HTTP 200)

Tal como la devolvió el servidor (verbatim, una sola línea, sin reformatear):

```xml
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/"><SOAP-ENV:Header/><SOAP-ENV:Body><ns2:ConsultarLibroPorIsbnResponse xmlns:ns2="http://uteq.edu.ec/pfc/libro-catalogo"><ns2:id>3</ns2:id><ns2:titulo>1984</ns2:titulo><ns2:autor>George Orwell</ns2:autor><ns2:isbn>978-0-451-52493-5</ns2:isbn><ns2:editorial>Penguin Random House Grupo Editorial</ns2:editorial><ns2:idioma>Inglés</ns2:idioma><ns2:estado>Disponible</ns2:estado><ns2:stock>4</ns2:stock></ns2:ConsultarLibroPorIsbnResponse></SOAP-ENV:Body></SOAP-ENV:Envelope>
```

Libro real consultado: **"1984" de George Orwell** (id=3 en la base de datos del seed).

### 1.4. Request/response SOAP real (caso: ISBN inexistente → SOAP Fault)

Mismo endpoint, ISBN `0000000000000` (no existe en la BD):

```xml
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/"><SOAP-ENV:Header/><SOAP-ENV:Body><SOAP-ENV:Fault><faultcode>SOAP-ENV:Client</faultcode><faultstring xml:lang="en">No existe un libro con el ISBN: 0000000000000</faultstring></SOAP-ENV:Fault></SOAP-ENV:Body></SOAP-ENV:Envelope>
```

HTTP status: **500**. Esto **no es un error genérico sin estructurar**: es el comportamiento correcto
y exigido por la especificación SOAP 1.1, que indica que el código HTTP debe ser 500 cuando el cuerpo
de la respuesta es un `<SOAP-ENV:Fault>` válido. La diferencia frente a un 500 genérico es que aquí el
cliente recibe un XML bien formado con `faultcode`/`faultstring` explicando la causa exacta
(`LibroNoEncontradoSoapException`, mecanismo `@SoapFault` + `SoapFaultAnnotationExceptionResolver`,
el patrón idiomático de Spring-WS), no una página de error HTML ni un stack trace crudo.

## 2. El equivalente REST del mismo caso de uso

Mismo libro (id=3, "1984"), vía `GET /api/v1/libros/{id}` (autenticado):

```json
{"success":true,"data":{"id":3,"titulo":"1984","descripcion":"Distopía que describe un régimen totalitario que controla cada aspecto de la vida de sus ciudadanos.","isbn":"978-0-451-52493-5","genero":"Ciencia ficción","autor":"George Orwell","anioPublicacion":1949,"editorialId":2,"editorialNombre":"Penguin Random House Grupo Editorial","idiomaId":2,"idiomaNombre":"Inglés","estadoId":1,"estadoNombre":"Disponible","stock":4,"createdAt":"2026-08-07T11:30:03.989513","updatedAt":"2026-08-07T11:30:03.989513"},"message":null,"errors":[],"meta":{}}
```

Nota honesta sobre la comparación: la API REST no tiene hoy un endpoint de búsqueda directa por ISBN
(el filtro existente en `GET /api/v1/libros?titulo=...` es por título). Se usó `GET /api/v1/libros/{id}`
con el id del mismo libro como el equivalente más cercano en alcance de datos, en vez de forzar una
comparación con un endpoint que no existe.

## 3. Comparación cuantitativa de tamaño de payload

Medido con `wc -c` sobre las respuestas reales capturadas arriba (solo el cuerpo, sin headers HTTP):

| Protocolo | Endpoint | Campos en la respuesta | Tamaño (bytes) |
|---|---|---|---|
| SOAP | `POST /ws` (ConsultarLibroPorIsbn) | 8 | 564 |
| REST | `GET /api/v1/libros/3` | 16 (incluye el envelope `ApiResponse`) | 550 |

**Hallazgo real, no esperado de antemano**: la respuesta REST es 14 bytes **más pequeña** que la SOAP,
a pesar de traer el doble de campos (16 vs 8). Esto no contradice la intuición general de que XML/SOAP
tiene más overhead que JSON/REST — la confirma con más matiz: cada elemento XML necesita una etiqueta de
apertura y cierre con el nombre completo calificado por namespace (p. ej. `<ns2:editorial>...</ns2:editorial>`,
24 caracteres de solo etiquetas para un campo), mientras que JSON representa el mismo dato como
`"editorial":"..."` (12 caracteres de sintaxis). El overhead por-elemento de XML es suficientemente alto
como para que, incluso con la mitad de los campos, SOAP termine pesando más que REST.

## 4. Configuración de seguridad

`/ws/**` se agregó explícitamente a `permitAll()` en `SecurityConfig` (aunque en la práctica ya estaba
cubierto por el catch-all `.anyRequest().permitAll()` preexistente, igual que ocurrió con las rutas de
Swagger). Se mantiene público a propósito: este servicio es una demostración técnica del patrón SOAP
para cumplir el resultado de aprendizaje del PDF, no un recurso de negocio que necesite protegerse por
rol. Confirmado explícitamente en el reporte de la tarea, no fue una decisión tomada en silencio.
