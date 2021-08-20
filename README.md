## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Testing with Blackbox tests

````
./gradlew clean build blackboxTest
````

### Running

!! Important note, you need to build an app first !!

To start the app all you need to have is a docker installed locally. Please use following command to start it with all needed dependencies:
`docker-compose -f ./antaeus/pleo-antaeus-blackbox-test/src/test/resources/bb-docker-compose.yml up --build`

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Postgresql](https://www.postgresql.org/) - Object relational database

Happy hacking 😁!

# Thinking process and the solution

## Introduction

My thinking process stared with initial scheduling problem. At the very beginning I was wondering if scheduling solution
should be implemented from scratch or to use some existing one. Initially wanted to have something which could be easy 
to use and with some required basic features:

* cluster friendly ( guarantees single execution across multiple nodes in a cluster )
* persistent tasks ( app deploy won't break current job schedule )
* cron expressions support ( just to easily handle given task date )

What was important for me was the simplicity and easiness of usage. Also, I didn't want to go with some big and invasive 
projects like for example Quartz as it could simply shadow the given solution. So eventually the outcome of investigation
and my thinking process was already existing solution: https://github.com/kagkarlsson/db-scheduler

## Coding assumptions

I think the most important decision is related to the way how initial scheduling process corresponds to particular
invoice charge. With current implementation initial scheduler goes throughout all pending invoices and for every each
creates totally separate charging task, with its own lifecycle, error handling and retry mechanism. With this approach I
was able to separate the "payment" logic and all possible error handling from the initial invoice picking job. I like
this approach for couple of reasons:

* From code design POV nice responsibility separation
* Network reliability or particular consistency errors won't affect the initial scheduling process
* Every charging task is independent, this adds nice flexibility for further improvements
* `PaymentCharge` is not bombarded with ton of request at given moment in time (also additionally, for each invoice
  charge, fake time shift was introduced, just to avoid possible internal ddos attack)
* Fallback process handled locally not globally

Now, dear reader, let me guide you through brief description of each service/class which was created by me:

* `BillingScheduler` - initial scheduler task configuration, place where main cron expression goes, at given time starts
  the invoice charge scheduling process

* `BillingService` - responsible for invoice charge task creation process, takes every invoice marked as pending and
  creates an execution with fake `x` seconds shift (just for example, to highlight possible network issues here) between
  very one, just to avoid for example potential overload of service behind PaymentProvider. For sake of simplicity
  decided  
  to go with simple for each statement for all pending invoices, however can image a scenario when this won't be
  enough  
  and some batching read approach could be needed.

* `PaymentChargeFailureHandler` - error handler for invoice charging process, with current implementation in case of
  `CurrencyMismatchException` or `CustomerNotFoundException` decided to stop the charging process for those invoices,
  having in mind it could mean some inconsistency issues across the entire platform. Also, can imagine that those
  invoices could be marked with some special `status` to highlight for example need of solving them manually. Another
  different scenario in case of `CurrencyMismatchException` could be additional exchange service and entire currency
  conversion process, however decided that this is outside of scope for this coding exercise for me.

  For "NetworkException" decided to implement simple retry mechanism with max attempt limit. Logging `warn` in case of
  possible self recovery from service side and error in other case. Also, at this point can think of possible status
  change after the final error, but this time decided to leave it with `PENDING` - will describe more in section
  possible improvements why.

* `PaymentChargeTask` - the main invoice charging component - scheduler task, in case of successfully charge marks
  invoice as paid, in case of not successful attempt together with `PaymentRecharger` starts simple retry process. After
  extending retry limit, marks invoice as unpaid.

* `PaymentRecharger` - reschedules the invoice charging process in case of not successful charge, a place to possibly
  notify system user that there was not enough money on the account/card, send some notification, etc.

* `SchedulerConfiguration` - simply scheduler configuration
* `OnDemandSchedulerTask` - interface for possible additional on demand tasks for scheduler
* `OnStartupScheduledTask` - interface for possible additional on startup tasks for scheduler

* `DateTimeProvider` - simply date time provider

* pleo-antaeus-blackbox-test - a bit more than integration test, won't relay on Javalin integration test approach, but  
  goes one level beyond and tests directly the exposed endpoints, no matter what solution is behind.  

## Possible improvements

* Dedicated endpoint for scheduling payment process, for example for invoices, affected by network issues 
* Gap/shift between executions moved to properties
* Maybe some batch read for pending invoices could be needed
* Database structure manage with solutions like Flyway/Liquibase

* For blackbox tests:
  * Drop the generated data and pass it explicitly to database for every test case
  * Drop fake random `PaymentProvider` implementation, add simple http client and with wiremock simulate
    different behaviours
