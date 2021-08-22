mvn clean compile assembly:single
cp target/sbconsumer-1.0-jar-with-dependencies.jar /d/projects/kafka/safeboda/docker/case-assigner/
# docker build --no-cache -t caseconsumer .
