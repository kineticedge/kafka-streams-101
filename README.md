
# Kafka Streams 101

* A project to demonstrate an example Kafka Streams application with no additional framwork.

* Project Contains

  * A Kakfa Producer application that takes input from a RESTful endpoint and publishes it Kafka topics.
  * A Kafka Streams application that assembles those messages into a aggregate and emits the results
  * A Kafka Consumer application that displays the aggregated message to the console.
  * A Kafka Admin client to display all the topics and their custom configuration (RESTful API)
  * A Docker Compose eco-system for running Apache Kafka
  * A Docker Compose to show how to scale instances with replicas, for easy testing of multiple instances; including an NGINX front-end for the producer and admin API.

