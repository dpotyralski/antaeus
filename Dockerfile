FROM adoptopenjdk/openjdk11:latest

ADD /pleo-antaeus-app/build/distributions/pleo-antaeus-app-1.0.tar ./antaeus/
ENTRYPOINT ./antaeus/pleo-antaeus-app-1.0/bin/pleo-antaeus-app

EXPOSE 7000
