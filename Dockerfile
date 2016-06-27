FROM ubuntu:16.04

RUN apt-get update && apt-get upgrade -y
RUN apt-get install openjdk-8-jre -y
RUN apt-get install curl unzip git -y

ENV ACTIVATOR_VERSION 1.3.10

RUN curl -O http://downloads.typesafe.com/typesafe-activator/$ACTIVATOR_VERSION/typesafe-activator-${ACTIVATOR_VERSION}-minimal.zip
RUN unzip typesafe-activator-${ACTIVATOR_VERSION}-minimal.zip -d / && rm typesafe-activator-${ACTIVATOR_VERSION}-minimal.zip && chmod a+x /activator-${ACTIVATOR_VERSION}-minimal/bin/activator
ENV PATH $PATH:/activator-${ACTIVATOR_VERSION}-minimal/bin

RUN cd /home && git clone https://github.com/BonarBeavis/Veritask.git
WORKDIR /home/Veritask
RUN activator clean stage
RUN sbt clean stage
