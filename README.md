# Options Analyzer
Project to analyze the technicals of stock options available in National Stock Exchange - India

#### Swagger UI   
https://127.0.0.1:8080/swagger-ui/index.html

### Build
```build
mvn clean install
``` 

#### Build only backend
```build only backend
mvn clean install -Pbackend-only
```

### Docker
```bash
docker build -t order-manager OrderManager   
```

```bash
docker-compose up -d
```
