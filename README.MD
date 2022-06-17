# Fs2 practice

## Run containers

```bash
$ docker-compose up
```

## Connect to Mongo container
```bash
$ docker exec -it mongo-container bash
```

## Enter Mongo shell
```bash
$ mongosh admin -u root -p 0987654321KnKn
```

## Select persons out of Mongo database
```bash
$ use fs2practice;
$ db.persons.find();
```

## Connect to Neo4j container
```bash
$ docker exec -it neo4j bash
```

## Enter Neo4j shell (username = neo4j, password = streams)
```bash
$ ./bin/cypher-shell 
```

## Select persons out of Neo4j database
```bash
$ MATCH r = (:Person)-[:OWNS]->(:Car) RETURN r;
```
