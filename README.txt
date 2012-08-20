To build:
mvn clean install -Declipse.addVersionToProjectName=true

To run with jetty:
cd web
mvn org.mortbay.jetty:maven-jetty-plugin:run

To develop with eclipse:
mvn clean install eclipse:clean eclipse:eclipse -DdownloadSources=true -DdownloadJavadocs=true -Declipse.addVersionToProjectName=true

WSDL
cd core
mvn install cxf-java2ws:java2ws
