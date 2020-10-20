# Pruebas webclient vs restemplate
Microservicio para comprobar intragraciones con servicios rest mediante webclient y restemplate
#### Tecnologías
Java 11

Spring Boot 2.3.4.RELEASE

#### Configuración
Editar el fichero *application.yml* las entradas.
> **urlSlowService**: Url del servicio lento para hacer las pruebas de llamadas bloqueantes. El código trae un ejemplo
> de servicio lento TweetController.getAllTweets() pero es importante si queremos usar este lanzarlo en otra instancia del 
>de las pruebas para mantener los hilos totalmente aislados a los de la prueba y obtener unos resultados fiables
>
> **baseUrlOms**: Url del servicio de oms.
>

Código realizado a partir del tutorial: https://www.baeldung.com/spring-webclient-resttemplate 