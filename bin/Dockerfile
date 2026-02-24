# Usamos el JRE de Java 17, que es más ligero para ejecución en producción
FROM eclipse-temurin:17-jre-alpine

# Establecemos el directorio de trabajo
WORKDIR /app

# Copiamos el JAR generado por Maven (Asegúrate de que el nombre coincida en tu carpeta target)
# Según tu imagen, el nombre es ms-gateway-0.0.1-SNAPSHOT.jar
COPY target/ms-gateway-*.jar app.jar

# El Gateway escucha por defecto en el puerto 8080
EXPOSE 8080

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
