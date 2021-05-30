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
|       Services.
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

### Project Summary (More detailed information could be find in the 'Way of implementing' section below)

Billing is implemented with cron job to schedule payment once on the first of every month.
Any exceptional situations shouldn't prevent job from charging invoices. Any retry mechanism could be helpful here.

Charging includes user balance changing and invoice status update. All payment tasks are executed asynchronously
and in parallel. Coroutines are used for asynchronous approach. User balance shouldn't be < 0, 
so it's necessary to block one database record (not whole logic block in the code) in general case. 
In current implementation payment tasks are grouped by customer id and charging is executed for every customer sequentially.

There were no requirements regarding charging conditions. So it's charged on the first of every month for the previous month.
Late invoices should be paid first.

Handling exception situation is implemented to just logging an error.

### Possible Project Improvements (Since I've been working closely with AWS recently, most of my technical and architectural decisions will be based on it.)

* Billing cron job
    * As for now cron job is executed only once in a month but application will be running always. It would be great
to run application only when it's needed. I would schedule AWS EC2/Lambda to call a service on demand. 
    * Cron job execution  will be retried after any exceptions. It would be great to have AWS Load Balancer 
to switch to other cron job in case of any server breaks.
    * As for now invoices are retrieved from database by status and date to make a payment for each of them. 
When customer subscribes to an application, invoice could be created asynchronously (added to a db). 
After that, message with necessary information could be added to a queue. When event (first of the every month) is triggered,
workers (AWS Lambdas f.e.) will pull a queue to retrieve an information what invoice to pay.
Any message broker could be used: Apache Kafka, JMS, AWS SQS etc.
* Exception situation
    * When CurrencyMismatchException appears, converter could be used to transform invoice value to make user payment possible 
(third party following exchange rate).
    * Handling exception situation is implemented to just logging an error. 
Maybe to use AWS Step Functions to make payment process transparent. It would be easy to understand 
on what step the error appears and to execute appropriate logic. Or to use event base approach.
* Payment
    * It would be great to use some third parties there.
    * Late invoices are paid first. Maybe to implement logic that makes possible
to pay for all invoices of a user which it could pay. In case of user has not enough balance, 
an email could be send to suggest to pay for a certain invoice with a deadline. Or to suggest using credit.
    * Performing payment puts into account timezone.
    * Implement discounts possibility.
* Storage
    * Invoice table will be huge in the future. Only later information is need, so archiving could be used here.
* Performance
    * Since invoices list could be huge, they could be retrieved in chunks (pagination). Indexes could be also helpful there.
    * Batch calls to a database.
* Authorization
* Additional services
    * Creating an invoice.
    * Implement service for balance increasing (f.e. after salary etc.)

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
* Implement service for balance increasing every months (f.e. after salary)
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
    * Load Balancer could be also helpful if server brokes.

#### 28 May (In total: 4.5h)
* Adds unit tests for data package
* Refactors

#### 29 May (In total: 3h)
* Adds unit tests for core package
* Updates BillingService

##### Points for improvements:
* Retrieve invoices using pagination

#### 30 May (In total: 3h)
* Refactors
* Adds sequence diagram for billing logic

##### Points for improvements:
* Creating an invoice with pending status when user subscribes to a service
* Performing payment putting into account timezone
* Authorization
