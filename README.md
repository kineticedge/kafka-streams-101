
# Kafka Streams 101

* A project to demonstrate an example Kafka Streams application with no additional framework.

* Project Contains

  * A Kafka Producer application that takes input from a RESTful endpoint and publishes it Kafka topics.
  * A Kafka Streams application that assembles those messages into an aggregate and emits the results
  * A Kafka Streams application that monitors the aggregate and reports on concerns (e.g. duplicate phone numbers).
  * A Kafka Consumer application that displays the aggregated message to the console.
  * A Kafka Admin client to display all the topics and their custom configuration (RESTful API).
  * A Docker Compose ecosystem for running Apache Kafka.
  * A Docker Compose to show how to scale instances with replicas, for easy testing of multiple instances; including an NGINX front-end for the producer and admin API.

* Important Considerations

  * This project uses no frameworks, but that doesn't mean you shouldn't or cant, but it is to showcase Kafka Client and Kafka Streams.
  * Lombok is used to reduce boilerplate code from this demonstration. This is not to imply you need to use it in production, but makes it easier to write this demonstration.
  * Jackson $type used for java serialization, this makes it easier to limit the custom serdes that would be written in the Kafka Streams DSL when using JSON w/out a schema.
  * The consumer project has two separate Kafka Consumers to show how you need to give each consumer a dedicated thread.
    * most microservice based consumers will be a single consumer.
    * you don't need a framework to get the application threading needed for your application.
  