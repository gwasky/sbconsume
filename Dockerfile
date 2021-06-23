FROM openjdk:8-jdk

ENV BROKER=kafka:29092
ENV OP_ENV=production

COPY target/sbconsumer-1.0-jar-with-dependencies.jar /app/sbconsumer.jar

CMD ["java","-jar","/app/sbconsumer.jar"]