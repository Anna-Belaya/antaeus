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

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus
docker run antaeus
```

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
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!

### Way of implementing

#### 24 May (In total: 4h)
* Installation, project deploy (1h)
* Libraries review (2h)
* Project review (1h)

##### Points to thinking about:
* Changing model structure (There's no date from which user started using application -- could be difficult for billing and defining if he payed or not.)
* Sqlite3 performance
* Could 'docker run' be faster?
* Split AntaeusDal for several interfaces: each works with one entity type.
* Invoice table will be huge in the future. Will all information be needed? Should it be archived?

#### 25 May (In total: 5h)
* Review documentation (exposed queries, cron job) (1h)
* Implementing simple version of application: (4h)
  * Adds balance for user
  * Negative cases aren't handled for now
  * Write simple cron job to test billing functionality

##### Points to thinking about:
* Problem when database row could be updated by multiple users. Some locking mechanism? 
* To use async approach? 
* Maybe use queue to avoid duplicates in payment process (something like publish–subscribe model)?
* Exceptions handling
* Customer could have many invoices. How to define order of payment?
* Queries processing in a more optimised way (indexes; grouping invoices by status and customer id to perform batch actions and increase database calls)

##### Points for improvements:
* Transform value in accordance in currency to make payment possible even in currencies don't match
* Implement mock service for balance increasing every months (f.e. after salary)
* Payment optimization. User could have many invoices. Service should define maximum possible invoices that could be paid in accordance with user balance.

#### 26 May (In total: 3h)
* Adds exception handling (0.5h)
* Review coroutines documentation (0.5h)
* Implements asynchronous approach (2h)

##### Points to thinking about:
* Issue when multiple users edit same row is still actual

##### Points for improvements:
* Send an email for any result from cron job, exceptions

#### 27 May (In total: 5h)
* Updates payment logic: invoice are grouped by customer id to prevent situation when two invoices are with the same customer id and balance of one user is changed (the same record in db) (1h)
* Refactored dal class to have a separate dal class for each entity type (0.25h)
* Adds created date field in the database. Currently invoices are fetched filtered by status and date (data for previous month) (2h)

##### Points to thinking about:
* I would increase concurrency during payment to speed up charging. Maybe use locking of database record for a certain customerId.
* Optimization of queries.
Read about issues in exposed library to perform batch calls.
* How to handle critical situations (f.e server isn't working)? 
    * Are retries and transactions enough? 
    * Maybe store invoices in external queue? It could help not to miss any invoices charging even after server break. 
    * Load Balancer could be also helpful if server broke.

#### 28 May (In total: 4.5h)
* Adds unit tests for data package
* Refactors

#### 29 May (In total: 3h)
* Adds unit tests for core package
* Updates BillingService

##### Points for improvements:
* Retrieve invoices using pagination
