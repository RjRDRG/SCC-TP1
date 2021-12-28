FROM tomcat:9.0-jdk11-openjdk
WORKDIR /usr/local/tomcat
ADD target/SCC-TP1-1.0-SNAPSHOT.war webapps
EXPOSE 8080